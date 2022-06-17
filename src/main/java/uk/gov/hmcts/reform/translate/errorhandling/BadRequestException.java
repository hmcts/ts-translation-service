package uk.gov.hmcts.reform.translate.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends ApiException {
    @Serial
    private static final long serialVersionUID = 7086418630129628219L;

    public BadRequestException(final String message) {
        super(message);
    }
}
