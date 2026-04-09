package ie.revenue.ccn.integration.business.component.revenue;

import ie.revenue.ccn.integration.business.component.njcsi.CcnQueue;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

public class CcnQueueConfig {

    @Getter
    private final CcnQueue ccnQueue;

    @Getter
    private final Map<String, String> messageTypeMap;

    @Getter
    private final boolean receivesMessages;

    public CcnQueueConfig(CcnQueue ccnQueue,
                          Map<String, String> messageTypeMap,
                          boolean receivesMessages) {

        this.ccnQueue = ccnQueue;
        this.messageTypeMap = messageTypeMap == null
                ? Collections.emptyMap()
                : messageTypeMap;
        this.receivesMessages = receivesMessages;
    }

    public boolean supportsMessageType(String revenueMessageType) {
        return messageTypeMap.containsKey(revenueMessageType);
    }

    public String getCcnMessageType(String revenueMessageType) {
        return messageTypeMap.get(revenueMessageType);
    }
}