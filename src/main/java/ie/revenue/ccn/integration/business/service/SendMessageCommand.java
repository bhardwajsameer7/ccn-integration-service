package ie.revenue.ccn.integration.business.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Immutable command carrying validated input for message send orchestration.
 */
public record SendMessageCommand(
        @NotBlank(message = "application type must not be blank")
        String applicationType,

        @NotBlank(message = "message type must not be blank")
        String messageType,

        @NotBlank(message = "Country code must not be blank")
        @Size(min = 2, max = 2, message = "countryCode must be a 2-letter ISO code")
        @Pattern(regexp = "[A-Z]{2}$", message = "countryCode must be a valid 2-letter ISO country code (uppercase)")
        String countryCode,

        @NotBlank(message = "Request payload must not be blank")
        String payload,

        String messageId,
        String correlationId
) {
}
