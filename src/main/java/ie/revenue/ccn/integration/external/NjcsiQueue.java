package ie.revenue.ccn.integration.external;
// This is organisation specific library.
//create to only remove compilation errors.

public class NjcsiQueue {
    public NjcsiQueueSender createSender() {
        return null;
    }

    public int getDepth() {
        return 1;
    }

    public NjcsiQueueBrowser createBrowser() {
        return null;
    }
}
