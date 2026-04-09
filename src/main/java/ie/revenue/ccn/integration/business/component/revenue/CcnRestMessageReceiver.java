package ie.revenue.ccn.integration.business.component.revenue;

import ie.revenue.ccn.integration.business.component.njcsi.CcnMessageReceiver;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;

public class CcnRestMessageReceiver implements CcnMessageReceiver {

    private final RestClient rest;

    public CcnRestMessageReceiver(RestClient rest) {
        this.rest = rest;
    }

    @Override
    public void receive(@NonNull String messageBody,
                        @NonNull String messageId,
                        String correlationId) {

        rest.post()
            .uri(uriBuilder -> addIdsAsQueryParams(uriBuilder, messageId, correlationId))
            .body(messageBody)
            .retrieve();
    }

    protected URI addIdsAsQueryParams(UriBuilder uriBuilder,
                                      String messageId,
                                      String correlationId) {

        uriBuilder.queryParam("messageId", messageId);

        if (correlationId != null) {
            uriBuilder.queryParam("correlationId", correlationId);
        }

        return uriBuilder.build();
    }
}