package ie.revenue.ccn.integration.exceptions;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(String applicationType) {
        super("No application configuration found for type: " + applicationType);
    }
}
