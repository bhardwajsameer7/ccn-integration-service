package ie.revenue.ccn.integration.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ie.revenue.ccncsi-integration")
@Getter
@Setter
public class CsiConfigProperties {

    private Map<String, ApplicationConfig> applications;

    @Getter
    @Setter
    public static class ApplicationConfig {
        private String applicationName;
        private String applicationKey;
        private String username;
        private String password;
        private String channel;
        private String destination;
        private boolean ccnGatewayEnabled;
        private RemoteApiProxy remoteApiProxy;
        private ReceivedMessagesTarget receivedMessagesTarget;
        private ReceivedMessagesConfig receivedMessagesConfig;
        private Map<String, CcnQueueConfig> queues;
        private Map<String, CountryMapping> countryMap;
    }

    @Getter
    @Setter
    public static class RemoteApiProxy {
        private String name;
        private String address;
        private int maxPoolSize;
        private int maxPoolIdleTime;
        private int poolShrinkingInterval;
    }

    @Getter
    @Setter
    public static class ReceivedMessagesTarget {
        private String url;
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class ReceivedMessagesConfig {
        private Duration frequency;
        private int maxMessagesPerPoll;
    }

    @Getter
    @Setter
    public static class CcnQueueConfig {
        private String queueName;
        private String replyTo;
        private Map<String, String> outboundMessageTypes;
        private boolean receiveMessages;
    }

    @Getter
    @Setter
    public static class CountryMapping {
        private String[] countries;
        private String format;
    }
}