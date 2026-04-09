package ie.revenue.ccn.integration.external;

public class NjcsiConnectionFactory {
    public static NjcsiConnectionFactory getInstance(Object inputStream) {
        return null;
    }


    // This is organisation specific library.
    //create to only remove compilation errors.

    public NjcsiCredentials createCredentials(String username, String password, String applicationName, String applicationKey) {
     return null;
    }

    public NjcsiConnection createConnection(NjcsiCredentials credentials) {
        return null;
    }
}
