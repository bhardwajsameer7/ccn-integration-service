package ie.revenue.ccn.integration.controller;

import ie.revenue.ccn.integration.business.service.OrchestrationService;
import ie.revenue.ccn.integration.model.MessageAndCorrelationId;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/restful/ccn/integration")
@Validated
@Slf4j
public class CcnIntegrationController {

    private final OrchestrationService orchestrationService;

    public CcnIntegrationController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/{applicationType}/{messageType}/{countryCode}")
    public ResponseEntity<CcnToClientResponse> consumeClientRequest(
            @PathVariable @NotBlank(message = "application type must not be blank") String applicationType,
            @PathVariable @NotBlank(message = "message type must not be blank") String messageType,
            @PathVariable @NotBlank(message = "Country code must not be blank")
            @Size(min = 2, max = 2, message = "countryCode must be a 2-letter ISO code")
            @Pattern(regexp = "[A-Z]{2}$", message = "countryCode must be a valid 2-letter ISO country code (uppercase)")
            String countryCode,
            @RequestHeader(name = "uniqueMessageId", required = false) String messageId,
            @RequestBody @NotBlank(message = "Request payload must not be blank") String payload
    ) throws Exception {

        log.info("Received request: applicationType={}, messageType={}, countryCode={}",
                applicationType, messageType, countryCode);

        MessageAndCorrelationId messageAndCorrelationId =
                orchestrationService.send(applicationType, messageType, countryCode, payload, messageId, messageId);

        log.info("Received response from orchestration service: applicationType={}, messageType={}, countryCode={}, messageId={}, correlationId={}",
                applicationType,
                messageType,
                countryCode,
                messageAndCorrelationId.getMessageId(),
                messageAndCorrelationId.getCorrelationId());

        // Return the response to client application
        return ResponseEntity.ok(
                new CcnToClientResponse(
                        messageAndCorrelationId.getMessageId(),
                        "SUCCESS",
                        "Message sent to CCN successfully",
                        "200"
                )
        );
    }

    /**
     * Checks the connectivity status of all CCN queues for the given application.
     * This method works generically for any registered application (e.g. DAC9, DAC4, DAC8, DAC2).
     *
     * @param applicationType the application to check (e.g. DAC9)
     * @return flat list of {@link QueueStatusResponse}, one entry per queue
     */
    @GetMapping("/{applicationType}/queue-status")
    public ResponseEntity<List<QueueStatusResponse>> checkQueueStatus(
            @PathVariable @NotBlank(message = "application type must not be blank") String applicationType
    ) {

        log.info("Checking queue status for application: {}", applicationType);

        List<QueueStatusResponse> response =
                orchestrationService.checkQueueStatus(applicationType);

        log.info("Queue status check completed for application: {}", applicationType);

        return ResponseEntity.ok(response);
    }
}