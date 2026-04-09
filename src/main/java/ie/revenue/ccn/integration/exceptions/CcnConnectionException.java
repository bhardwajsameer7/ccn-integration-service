package ie.revenue.ccn.integration.exceptions;

public class CcnConnectionException extends RuntimeException {

    public CcnConnectionException(String message) {
        super(message);
    }

    public CcnConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
