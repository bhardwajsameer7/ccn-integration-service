package ie.revenue.ccn.integration.business.component.njcsi;

import lombok.NonNull;

@FunctionalInterface
public interface CcnMessageReceiver {
    void receive (@NonNull String messageBody, @NonNull String messageld, String correlationId);
}

