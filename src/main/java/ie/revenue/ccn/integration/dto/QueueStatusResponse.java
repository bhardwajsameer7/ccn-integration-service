package ie.revenue.ccn.integration.dto;

import lombok.Data;

@Data
public class QueueStatusResponse {

    private String queueName;
    private Integer queueDepth;
    private String exception;
}