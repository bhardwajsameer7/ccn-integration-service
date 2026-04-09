package ie.revenue.ccn.integration.external;

import java.security.interfaces.EdECPrivateKey;
// This is organisation specific library.
//create to only remove compilation errors.

public class NjcsiDequeuedMessage {
    public boolean getQueueMessageType() {
        return true;
    }

    public byte[] getData() {
        return null;
    }

    public byte[] getMessageID() {
        return null;
    }

    public byte[] getCorrelationID() {
        return null;
    }

    public Object getReportType() {
        return null;
    }

    public Object getMessageType() {
        return null;
    }
}
