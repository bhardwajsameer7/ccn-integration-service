package ie.revenue.ccn.integration.external;

// This is organisation specific library.
//create to only remove compilation errors.

public class NjcsiQueueBrowser implements AutoCloseable {
    @Override
    public void close() throws Exception {

    }

    public NjcsiDequeuedMessage browse(int i, boolean b) {
        return null;
    }

    public void delete() {
    }
}
