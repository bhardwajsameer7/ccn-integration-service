package ie.revenue.ccn.integration.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ie.revenue.ccncsi-integration")
@Getter
@Setter
@Validated
public class CsiConfigProperties {

    @NotEmpty
    private Map<String, @Valid ApplicationConfig> applications;

    @Getter
    @Setter
    public static class ApplicationConfig {
        @NotBlank
        private String applicationName;
        @NotBlank
        private String applicationKey;
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        private String channel;
        @NotBlank
        private String destination;
        private boolean ccnGatewayEnabled;
        @NotNull
        @Valid
        private RemoteApiProxy remoteApiProxy;
        @NotNull
        @Valid
        private ReceivedMessagesTarget receivedMessagesTarget;
        @NotNull
        @Valid
        private ReceivedMessagesConfig receivedMessagesConfig;
        @NotEmpty
        private Map<String, @Valid CcnQueueConfig> queues;
        @NotEmpty
        private Map<String, @Valid CountryMapping> countryMappings;
    }

    @Getter
    @Setter
    public static class RemoteApiProxy {
        @NotBlank
        private String name;
        @NotBlank
        private String address;
        private int maxPoolSize;
        private int maxPoolIdleTime;
        private int poolShrinkingInterval;
    }

    @Getter
    @Setter
    public static class ReceivedMessagesTarget {
        @NotBlank
        private String url;
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Getter
    @Setter
    public static class ReceivedMessagesConfig {
        @NotNull
        private Duration frequency;
        private int maxMessagesPerPoll;
    }

    @Getter
    @Setter
    public static class CcnQueueConfig {
        @NotBlank
        private String queueName;
        private String replyTo;
        private Map<String, String> outboundMessageTypes;
        private boolean receiveMessages;
    }

    @Getter
    @Setter
    public static class CountryMapping {
        @NotNull
        private String[] countries;
        @NotBlank
        private String format;
    }
}
