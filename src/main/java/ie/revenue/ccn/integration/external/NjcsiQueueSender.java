package ie.revenue.ccn.integration.external;
// This is organisation specific library.
//create to only remove compilation errors.

public class NjcsiQueueSender implements AutoCloseable {
    @Override
    public void close() throws Exception {

    }

    public void passMessageID(boolean b) {

    }

    public void passCorrelationID(boolean b) {
    }

    public void setReplyToQueue(String replyToQueueName, NjcsiDestination njcsiDestination) {

    }

    public void send(NjcsiEnqueuedMessage message) {
    }
}
