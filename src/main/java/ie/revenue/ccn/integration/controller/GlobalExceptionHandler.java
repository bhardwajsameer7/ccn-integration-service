package ie.revenue.ccn.integration.controller;

import ie.revenue.ccn.integration.exceptions.ApplicationNotFoundException;
import ie.revenue.ccn.integration.exceptions.CcnConnectionException;
import ie.revenue.ccn.integration.exceptions.CcnGatewayUnavailableException;
import ie.revenue.ccn.integration.exceptions.CountryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ProblemDetail handleApplicationNotFound(ApplicationNotFoundException ex) {
        return buildProblem(HttpStatus.NOT_FOUND, "Application not found", ex);
    }

    @ExceptionHandler(CountryNotFoundException.class)
    public ProblemDetail handleCountryNotFound(CountryNotFoundException ex) {
        return buildProblem(HttpStatus.NOT_FOUND, "Country mapping not found", ex);
    }

    @ExceptionHandler(CcnGatewayUnavailableException.class)
    public ProblemDetail handleGatewayUnavailable(CcnGatewayUnavailableException ex) {
        return buildProblem(HttpStatus.SERVICE_UNAVAILABLE, "CCN gateway unavailable", ex);
    }

    @ExceptionHandler({CcnConnectionException.class, IllegalArgumentException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Unable to process request", ex);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnhandled(Exception ex) {
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", ex);
    }

    private ProblemDetail buildProblem(HttpStatus status, String title, Exception ex) {
        log.error("{}: {}", title, ex.getMessage(), ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        detail.setTitle(title);
        return detail;
    }
}
