package ie.revenue.ccn.integration.business.service;

import ie.revenue.ccn.integration.business.component.revenue.CcnIntegrationLayer;
import ie.revenue.ccn.integration.exceptions.ApplicationNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Centralized registry for application integration layers.
 * Keeps map-lookup logic out of orchestration/business flows.
 */
@Component
public class ApplicationIntegrationRegistry {

    private final Map<String, CcnIntegrationLayer> ccnIntegrationLayerMap;

    public ApplicationIntegrationRegistry(Map<String, CcnIntegrationLayer> ccnIntegrationLayerMap) {
        this.ccnIntegrationLayerMap = ccnIntegrationLayerMap;
    }

    public CcnIntegrationLayer getByApplicationType(String applicationType) {
        CcnIntegrationLayer ccnIntegrationLayer = ccnIntegrationLayerMap.get(applicationType);
        if (ccnIntegrationLayer == null) {
            throw new ApplicationNotFoundException(applicationType);
        }
        return ccnIntegrationLayer;
    }

    public Set<String> getApplicationTypes() {
        return ccnIntegrationLayerMap.keySet();
    }
}
