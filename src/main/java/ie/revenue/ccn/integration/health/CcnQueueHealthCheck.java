package ie.revenue.ccn.integration.health;

import ie.revenue.ccn.integration.business.service.OrchestrationService;
import ie.revenue.ccn.integration.configuration.CsiConfigProperties;
import ie.revenue.ccn.integration.dto.QueueStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CcnQueueHealthCheck {

    private final OrchestrationService orchestrationService;
    private final CsiConfigProperties csiConfigProperties;

    public CcnQueueHealthCheck(OrchestrationService orchestrationService,
                               CsiConfigProperties csiConfigProperties) {
        this.orchestrationService = orchestrationService;
        this.csiConfigProperties = csiConfigProperties;
    }

    @Scheduled(
            initialDelayString = "${ccn.queue.health-check.initial-delay}",
            fixedRateString = "${ccn.queue.health-check.interval}"
    )
    public void checkQueueConnectivity() {

        log.info("Performing scheduled CCN queue connectivity check...");

        orchestrationService.getApplicationTypes().forEach(applicationName -> {

            CsiConfigProperties.ApplicationConfig appConfig =
                    csiConfigProperties.getApplications().get(applicationName);

            if (appConfig == null || !appConfig.isCcnGatewayEnabled()) {
                log.info("Skipping queue check for application={} as CCN Gateway is disabled", applicationName);
                return;
            }

            try {
                List<QueueStatusResponse> statuses =
                        orchestrationService.checkQueueStatus(applicationName);

                statuses.forEach(status -> {
                    if (status.getException() != null) {
                        log.warn(
                                "SCHEDULED queue check FAILED: application={}, queue={}, error={}",
                                applicationName,
                                status.getQueueName(),
                                status.getException()
                        );
                    } else {
                        log.info(
                                "SCHEDULED queue check OK: application={}, queue={}, depth={}",
                                applicationName,
                                status.getQueueName(),
                                status.getQueueDepth()
                        );
                    }
                });

            } catch (Exception e) {
                log.warn(
                        "SCHEDULED queue check UNAVAILABLE: application={}, reason={}",
                        applicationName,
                        e.getMessage()
                );
            }
        });

        log.info("Scheduled queue connectivity check complete.");
    }
}
