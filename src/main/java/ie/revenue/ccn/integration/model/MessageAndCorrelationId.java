package ie.revenue.ccn.integration.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageAndCorrelationId {

    private final String messageId;
    private final String correlationId;

}
