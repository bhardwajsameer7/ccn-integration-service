package ie.revenue.ccn.integration.business.component.revenue;

import ie.revenue.ccn.integration.business.component.njcsi.CcnConnection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class CcnIntegrationLayer {

    @Getter
    private final String applicationName;

    @Getter
    private final boolean isCcnGatewayEnabled;

    private final CcnConnection ccnConnection;

    // Efficient lookup: messageType → queue config
    private final Map<String, CcnQueueConfig> messageTypeToQueueConfigMap;

    // Precomputed list for iteration (browse/status)
    private final List<CcnQueueConfig> precomputedCcnQueueConfigList;

    public CcnIntegrationLayer(String applicationName,
                               boolean isCcnGatewayEnabled,
                               CcnConnection ccnConnection,
                               Map<String, CcnQueueConfig> messageTypeToQueueConfigMap,
                               List<CcnQueueConfig> precomputedCcnQueueConfigList) {

        this.applicationName = applicationName;
        this.isCcnGatewayEnabled = isCcnGatewayEnabled;
        this.ccnConnection = ccnConnection;
        this.messageTypeToQueueConfigMap = messageTypeToQueueConfigMap;
        this.precomputedCcnQueueConfigList = precomputedCcnQueueConfigList;
    }

    public MessageAndCorrelationId send(String revenueMessageType,
                                        String destination,
                                        String messageBody,
                                        String messageId,
                                        String correlationId) {

        CcnQueueConfig ccnQueueConfig = messageTypeToQueueConfigMap.get(revenueMessageType);

        if (ccnQueueConfig == null) {
            log.error("No queue configuration found for message type: {}", revenueMessageType);
            throw new IllegalArgumentException(
                    "No queue configuration found for message type: " + revenueMessageType
            );
        }

        String ccnMessageType = ccnQueueConfig.getCcnMessageType(revenueMessageType);

        return ccnQueueConfig.getCcnQueue()
                .send(ccnConnection, destination, ccnMessageType, messageBody, messageId, correlationId);
    }

    public void browse() {
        for (CcnQueueConfig ccnQueueConfig : precomputedCcnQueueConfigList) {
            if (ccnQueueConfig.isReceivesMessages()) {
                try {
                    ccnQueueConfig.getCcnQueue().browse(ccnConnection);
                } catch (Exception e) {
                    log.error("Error browsing queue: {}",
                            ccnQueueConfig.getCcnQueue().getQueueName(), e);
                }
            }
        }
    }

    /**
     * Checks connectivity status of all queues configured for this application.
     * Only opens a connection and reads queue depth.
     */
    public List<QueueStatusResponse> checkQueueStatus() {

        List<QueueStatusResponse> statuses = new ArrayList<>();

        for (CcnQueueConfig ccnQueueConfig : precomputedCcnQueueConfigList) {

            CcnQueue ccnQueue = ccnQueueConfig.getCcnQueue();

            QueueStatusResponse status = new QueueStatusResponse();
            status.setQueueName(ccnQueue.getQueueName());

            try {
                int depth = ccnQueue.checkDepth(ccnConnection);
                status.setQueueDepth(depth);
                status.setException(null);

            } catch (Exception e) {
                log.error("Queue connection check failed for queue: {}",
                        ccnQueue.getQueueName(), e);

                status.setQueueDepth(0);
                status.setException(e.getMessage());
            }

            statuses.add(status);
        }

        return statuses;
    }
}