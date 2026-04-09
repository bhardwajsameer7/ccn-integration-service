package ie.revenue.ccn.integration.controller;

import ie.revenue.ccn.integration.business.service.OrchestrationService;
import ie.revenue.ccn.integration.business.service.SendMessageCommand;
import ie.revenue.ccn.integration.dto.CcnToClientResponse;
import ie.revenue.ccn.integration.dto.QueueStatusResponse;
import ie.revenue.ccn.integration.model.MessageAndCorrelationId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    ) {

        log.info("Received request: applicationType={}, messageType={}, countryCode={}",
                applicationType, messageType, countryCode);

        SendMessageCommand command = new SendMessageCommand(
                applicationType,
                messageType,
                countryCode,
                payload,
                messageId,
                messageId
        );

        MessageAndCorrelationId response = orchestrationService.send(command);

        log.info("Received response from orchestration service: applicationType={}, messageType={}, countryCode={}, messageId={}, correlationId={}",
                applicationType,
                messageType,
                countryCode,
                response.getMessageId(),
                response.getCorrelationId());

        return ResponseEntity.ok(
                new CcnToClientResponse(
                        response.getMessageId(),
                        "SUCCESS",
                        "Message sent to CCN successfully",
                        "200"
                )
        );
    }

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
