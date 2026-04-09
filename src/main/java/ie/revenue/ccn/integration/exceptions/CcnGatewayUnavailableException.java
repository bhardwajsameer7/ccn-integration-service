package ie.revenue.ccn.integration.exceptions;

public class CcnGatewayUnavailableException extends RuntimeException {

    public CcnGatewayUnavailableException(String applicationType) {
        super("CCN Gateway is not enabled for application type: " + applicationType);
    }
}
