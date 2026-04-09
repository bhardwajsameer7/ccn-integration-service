package ie.revenue.ccn.integration.business.service;

import ie.revenue.ccn.integration.business.component.revenue.CcnIntegrationLayer;
import ie.revenue.ccn.integration.dto.QueueStatusResponse;
import ie.revenue.ccn.integration.exceptions.ApplicationNotFoundException;
import ie.revenue.ccn.integration.exceptions.CcnGatewayUnavailableException;
import ie.revenue.ccn.integration.model.MessageAndCorrelationId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public MessageAndCorrelationId send(SendMessageCommand command) {

        log.info("Start processing request in OrchestrationService: applicationType={}, uniqueMessageId={}",
                command.applicationType(), command.messageId());

        CcnIntegrationLayer ccnIntegrationLayer = ccnIntegrationLayerMap.get(command.applicationType());

        if (ccnIntegrationLayer == null) {
            log.error("No ccnIntegrationLayer found with type: {}", command.applicationType());
            throw new ApplicationNotFoundException(command.applicationType());
        }

        if (!ccnIntegrationLayer.isCcnGatewayEnabled()) {
            log.error("CCN Gateway is not enabled for application type: {}", command.applicationType());
            throw new CcnGatewayUnavailableException(command.applicationType());
        }

        String destination = countryMappingService
                .getDestinationByApplicationTypeAndCountry(command.applicationType(), command.countryCode());

        return ccnIntegrationLayer.send(
                command.messageType(),
                destination,
                command.payload(),
                command.messageId(),
                command.correlationId()
        );
    }

    public Set<String> getApplicationTypes() {
        return ccnIntegrationLayerMap.keySet();
    }

    public List<QueueStatusResponse> checkQueueStatus(String applicationType) {

        log.info("Checking queue status for application: {}", applicationType);

        CcnIntegrationLayer ccnIntegrationLayer =
                ccnIntegrationLayerMap.get(applicationType);

        if (ccnIntegrationLayer == null) {
            throw new ApplicationNotFoundException(applicationType);
        }

        return ccnIntegrationLayer.checkQueueStatus();
    }
}
