package uk.gov.hmcts.reform.translate.errorhandling;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
