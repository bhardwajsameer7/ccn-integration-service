package ie.revenue.ccn.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CcnToClientResponse {

    private String uniqueMessageId;
    private String status;
    private String statusMessage;
    private String statusCode;
}