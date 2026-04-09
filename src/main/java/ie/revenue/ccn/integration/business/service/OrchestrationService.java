package ie.revenue.ccn.integration.business.service;

import ie.revenue.ccn.integration.business.component.revenue.CcnIntegrationLayer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.Map;

@Service
@Slf4j
public class OrchestrationService {

    private final CountryMappingService countryMappingService;
    private final Map<String, CcnIntegrationLayer> ccnIntegrationLayerMap;

    public OrchestrationService(CountryMappingService countryMappingService,
                                Map<String, CcnIntegrationLayer> ccnIntegrationLayerMap) {
        this.countryMappingService = countryMappingService;
        this.ccnIntegrationLayerMap = ccnIntegrationLayerMap;
    }

    /**
     * This method processes the incoming request from client application and sends the message
     * to the correct CCN queue based on the application type, message type and country code.
     *
     * 1. Find the CcnIntegrationLayer for the given application type.
     * 2. Uses countryMappingService to get destination.
     * 3. Delegates send to integration layer.
     */
    public MessageAndCorrelationId send(String applicationType,
                                        String messageType,
                                        String countryCode,
                                        String payload,
                                        String messageId,
                                        String correlationId) {

        log.info("Start processing request in OrchestrationService: applicationType={}, uniqueMessageId={}",
                applicationType, messageId);

        CcnIntegrationLayer ccnIntegrationLayer = ccnIntegrationLayerMap.get(applicationType);

        if (ccnIntegrationLayer == null) {
            log.error("No ccnIntegrationLayer found with type: {}", applicationType);
            throw new IllegalArgumentException(
                    "No ccnIntegrationLayer found with type: " + applicationType
            );
        }

        if (!ccnIntegrationLayer.isCcnGatewayEnabled()) {
            log.error("CCN Gateway is not enabled for application type: {}", applicationType);
            throw new CcnGatewayUnavailableException(applicationType);
        }

        String destination = countryMappingService
                .getDestinationByApplicationTypeAndCountry(applicationType, countryCode);

        return ccnIntegrationLayer.send(
                messageType,
                destination,
                payload,
                messageId,
                correlationId
        );
    }

    /**
     * Returns all registered application types (e.g. DAC9, DAC4, DAC8)
     */
    public Set<String> getApplicationTypes() {
        return ccnIntegrationLayerMap.keySet();
    }

    /**
     * Checks queue connectivity status for a given application
     */
    public List<QueueStatusResponse> checkQueueStatus(String applicationType) {

        log.info("Checking queue status for application: {}", applicationType);

        CcnIntegrationLayer ccnIntegrationLayer =
                ccnIntegrationLayerMap.get(applicationType);

        if (ccnIntegrationLayer == null) {
            throw new IllegalArgumentException(
                    "No application found with name: " + applicationType
            );
        }

        return ccnIntegrationLayer.checkQueueStatus();
    }
}