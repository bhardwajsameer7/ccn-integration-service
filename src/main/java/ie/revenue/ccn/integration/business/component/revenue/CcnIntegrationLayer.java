package ie.revenue.ccn.integration.business.component.revenue;

import ie.revenue.ccn.integration.business.component.njcsi.CcnConnection;
import ie.revenue.ccn.integration.business.component.njcsi.CcnQueue;
import ie.revenue.ccn.integration.dto.QueueStatusResponse;
import ie.revenue.ccn.integration.model.MessageAndCorrelationId;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CcnIntegrationLayer {

    @Getter
    private final String applicationName;

    @Getter
    private final boolean ccnGatewayEnabled;

    private final CcnConnection ccnConnection;

    private final Map<String, CcnQueueConfig> messageTypeToQueueConfigMap;
    private final List<CcnQueueConfig> precomputedCcnQueueConfigList;

    public CcnIntegrationLayer(String applicationName,
                               boolean ccnGatewayEnabled,
                               CcnConnection ccnConnection,
                               Map<String, CcnQueueConfig> messageTypeToQueueConfigMap,
                               List<CcnQueueConfig> precomputedCcnQueueConfigList) {

        this.applicationName = applicationName;
        this.ccnGatewayEnabled = ccnGatewayEnabled;
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
