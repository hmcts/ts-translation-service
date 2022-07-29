package uk.gov.hmcts.reform.translate.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class EnglishPhraseUniqueConstraintException extends ApiException {
    public EnglishPhraseUniqueConstraintException(String message, Throwable cause) {
        super(message, cause);
    }
}
