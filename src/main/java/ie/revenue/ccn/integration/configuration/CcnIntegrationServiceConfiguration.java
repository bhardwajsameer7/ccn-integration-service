package ie.revenue.ccn.integration.configuration;

import ie.revenue.ccn.integration.business.component.njcsi.CcnConnection;
import ie.revenue.ccn.integration.business.component.njcsi.CcnMessageReceiver;
import ie.revenue.ccn.integration.business.component.njcsi.CcnQueue;
import ie.revenue.ccn.integration.business.component.njcsi.CsiConfigFileGenerator;
import ie.revenue.ccn.integration.business.component.revenue.CcnIntegrationLayer;
import ie.revenue.ccn.integration.business.component.revenue.CcnQueueConfig;
import ie.revenue.ccn.integration.business.component.revenue.CcnRestMessageReceiver;
import ie.revenue.ccn.integration.business.service.CountryMappingService;
import ie.revenue.ccn.integration.external.NjcsiConnectionFactory;
import ie.revenue.ccn.integration.external.NjcsiInternalException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class CcnIntegrationServiceConfiguration implements SchedulingConfigurer {

    private final CsiConfigProperties csiConfigProperties;
    private final MeterRegistry meterRegistry;
    private final TaskScheduler taskScheduler;
    private final RestClient.Builder restClientBuilder;
    private final ObjectProvider<Map<String, CcnIntegrationLayer>> integrationLayersProvider;

    public CcnIntegrationServiceConfiguration(CsiConfigProperties csiConfigProperties,
                                              MeterRegistry meterRegistry,
                                              TaskScheduler taskScheduler,
                                              RestClient.Builder restClientBuilder,
                                              ObjectProvider<Map<String, CcnIntegrationLayer>> integrationLayersProvider) {
        this.csiConfigProperties = csiConfigProperties;
        this.meterRegistry = meterRegistry;
        this.taskScheduler = taskScheduler;
        this.restClientBuilder = restClientBuilder;
        this.integrationLayersProvider = integrationLayersProvider;
    }

    @Bean
    public CountryMappingService countryMappingService() {

        Map<String, Map<String, String>> applicationTypeToCountryToDestinationMap = new HashMap<>();

        csiConfigProperties.getApplications().forEach((applicationType, appConfig) -> {

            if (appConfig.getCountryMappings() == null || appConfig.getCountryMappings().isEmpty()) {
                log.warn("No country mapping defined for application type: {}", applicationType);
                return;
            }

            Map<String, String> countryToDestination = new HashMap<>();
            appConfig.getCountryMappings().forEach((mappingName, countryMapping) -> {

                if (countryMapping != null &&
                    countryMapping.getCountries() != null &&
                    countryMapping.getFormat() != null) {

                    for (String country : countryMapping.getCountries()) {
                        countryToDestination.put(country,
                                countryMapping.getFormat().formatted(country));
                    }
                }
            });

            applicationTypeToCountryToDestinationMap.put(applicationType, countryToDestination);
        });

        return new CountryMappingService(applicationTypeToCountryToDestinationMap);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler);

        Map<String, CcnIntegrationLayer> layers = integrationLayersProvider.getIfAvailable(HashMap::new);

        csiConfigProperties.getApplications().forEach((applicationType, appConfig) -> {

            if (!appConfig.isCcnGatewayEnabled()) {
                log.info("CCN Gateway disabled for {}", applicationType);
                return;
            }

            CcnIntegrationLayer layer = layers.get(applicationType);
            if (layer == null) {
                log.warn("No integration layer registered for application type {}", applicationType);
                return;
            }

            taskRegistrar.addFixedDelayTask(
                    layer::browse,
                    appConfig.getReceivedMessagesConfig().getFrequency()
            );
        });
    }

    @Bean
    public Map<String, CsiConfigFileGenerator> csiConfigFileMapByApplicationType() {

        Map<String, CsiConfigFileGenerator> map = new HashMap<>();

        csiConfigProperties.getApplications().forEach((applicationType, appConfig) -> map.put(applicationType,
                new CsiConfigFileGenerator(
                        appConfig.getApplicationName(),
                        appConfig.getRemoteApiProxy().getName(),
                        appConfig.getRemoteApiProxy().getAddress(),
                        appConfig.getRemoteApiProxy().getMaxPoolSize(),
                        appConfig.getRemoteApiProxy().getMaxPoolIdleTime(),
                        appConfig.getRemoteApiProxy().getPoolShrinkingInterval()
                )));

        return map;
    }

    @Bean
    public Map<String, NjcsiConnectionFactory> njcsiConnectionFactoryMapByApplicationType(
            Map<String, CsiConfigFileGenerator> csiConfigFileMapByApplicationType) {

        Map<String, NjcsiConnectionFactory> map = new HashMap<>();

        csiConfigProperties.getApplications().forEach((applicationType, appConfig) -> {
            try {
                map.put(applicationType,
                        NjcsiConnectionFactory.getInstance(
                                csiConfigFileMapByApplicationType
                                        .get(applicationType)
                                        .asInputStream()
                        ));
            } catch (NjcsiInternalException e) {
                throw new RuntimeException("Unable to create NJCSI connection factory for " + applicationType, e);
            }
        });

        return map;
    }

    @Bean
    public Map<String, CcnIntegrationLayer> ccnIntegrationLayersByApplicationType(
            Map<String, NjcsiConnectionFactory> njcsiConnectionFactoryMapByApplicationType) {

        Map<String, CcnIntegrationLayer> map = new HashMap<>();

        csiConfigProperties.getApplications().forEach((applicationType, appConfig) -> {

            CcnConnection connection = new CcnConnection(
                    appConfig.getApplicationName(),
                    appConfig.getApplicationKey(),
                    appConfig.getUsername(),
                    appConfig.getPassword(),
                    njcsiConnectionFactoryMapByApplicationType.get(applicationType)
            );

            RestClient restClient = restClientBuilder.clone()
                    .requestInterceptor(new BasicAuthenticationInterceptor(
                            appConfig.getReceivedMessagesTarget().getUsername(),
                            appConfig.getReceivedMessagesTarget().getPassword()))
                    .baseUrl(appConfig.getReceivedMessagesTarget().getUrl())
                    .build();

            CcnMessageReceiver receiver = new CcnRestMessageReceiver(restClient);

            Map<String, CcnQueueConfig> queueConfigMap =
                    buildCcnQueueConfigs(appConfig, receiver);

            Map<String, CcnQueueConfig> messageTypeMap =
                    buildMessageTypeToQueueConfigMap(queueConfigMap);

            List<CcnQueueConfig> list = new ArrayList<>(queueConfigMap.values());

            CcnIntegrationLayer layer = new CcnIntegrationLayer(
                    applicationType,
                    appConfig.isCcnGatewayEnabled(),
                    connection,
                    messageTypeMap,
                    list
            );

            map.put(applicationType, layer);
        });

        return map;
    }

    private Map<String, CcnQueueConfig> buildMessageTypeToQueueConfigMap(
            Map<String, CcnQueueConfig> queueMap) {

        Map<String, CcnQueueConfig> result = new HashMap<>();

        for (CcnQueueConfig config : queueMap.values()) {
            for (String messageType : config.getMessageTypeMap().keySet()) {

                if (result.containsKey(messageType)) {
                    throw new IllegalArgumentException("Duplicate message type: " + messageType);
                }

                result.put(messageType, config);
            }
        }

        return result;
    }

    private Map<String, CcnQueueConfig> buildCcnQueueConfigs(
            CsiConfigProperties.ApplicationConfig appConfig,
            CcnMessageReceiver receiver) {

        Map<String, CcnQueueConfig> map = new HashMap<>();

        appConfig.getQueues().forEach((queueName, config) -> {

            validateCcnQueueConfig(queueName, config, appConfig.getQueues());

            String replyTo = config.getReplyTo() == null ? null :
                    appConfig.getQueues().get(config.getReplyTo()).getQueueName();

            CcnQueue queue = new CcnQueue(
                    config.getQueueName(),
                    appConfig.getApplicationName(),
                    replyTo,
                    appConfig.getDestination(),
                    appConfig.getReceivedMessagesConfig().getMaxMessagesPerPoll(),
                    receiver,
                    meterRegistry
            );

            map.put(queueName,
                    new CcnQueueConfig(
                            queue,
                            config.getOutboundMessageTypes(),
                            config.isReceiveMessages()
                    ));
        });

        return map;
    }

    private void validateCcnQueueConfig(String queueName,
                                        CsiConfigProperties.CcnQueueConfig config,
                                        Map<String, CsiConfigProperties.CcnQueueConfig> queues) {

        if (config.getOutboundMessageTypes() != null && config.getReplyTo() == null) {
            throw new IllegalArgumentException(
                    "Outbound types defined but replyTo missing for queue " + queueName);
        }

        if (config.getReplyTo() != null && !queues.containsKey(config.getReplyTo())) {
            throw new IllegalArgumentException(
                    "Invalid replyTo for queue " + queueName);
        }
    }
}
