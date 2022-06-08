package uk.gov.hmcts.reform.translate.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
public class RequestErrorException extends IllegalStateException {

    public static final String ERROR_MESSAGE =
        "The request should be from a valid service or the User does not have '%s' role";

    @Serial
    private static final long serialVersionUID = -8673990147686728273L;

    public RequestErrorException(String missingRoleName) {
        super(String.format(ERROR_MESSAGE, missingRoleName));
    }
}
