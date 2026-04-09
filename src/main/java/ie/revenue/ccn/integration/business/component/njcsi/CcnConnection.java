package ie.revenue.ccn.integration.business.component.njcsi;

import ie.revenue.ccn.integration.exceptions.CcnConnectionException;
import ie.revenue.ccn.integration.external.NjcsiConnection;
import ie.revenue.ccn.integration.external.NjcsiConnectionFactory;
import ie.revenue.ccn.integration.external.NjcsiCredentials;
import ie.revenue.ccn.integration.external.NjcsiException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class CcnConnection {

    private final String applicationName;
    private final String applicationKey;
    private final String username;
    private final String password;
    private final NjcsiConnectionFactory njcsiConnectionFactory;

    public CcnConnection(String applicationName,
                         String applicationKey,
                         String username,
                         String password,
                         NjcsiConnectionFactory njcsiConnectionFactory) {
        this.applicationName = applicationName;
        this.applicationKey = applicationKey;
        this.username = username;
        this.password = password;
        this.njcsiConnectionFactory = njcsiConnectionFactory;
    }

    public <T> T doWithConnection(Function<NjcsiConnection, T> connectionConsumer) {
        try {
            NjcsiCredentials credentials = njcsiConnectionFactory.createCredentials(
                    username,
                    password,
                    applicationName,
                    applicationKey
            );

            log.info("Creating connection with credentials for application {} with username {}",
                    applicationName, username);

            try (NjcsiConnection connection = njcsiConnectionFactory.createConnection(credentials)) {
                return connectionConsumer.apply(connection);
            }

        } catch (NjcsiException e) {
            log.error("Error working with NJCSI connection", e);
            throw new CcnConnectionException("Error working with NJCSI connection");

        } catch (Exception e) {
            throw new CcnConnectionException("Error working with NJCSI connection");
        }
    }
}