package uk.gov.hmcts.reform.translate.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.Serializable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.BAD_SCHEMA;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ApiException.class})
    @ResponseBody
    public ResponseEntity<HttpError<Serializable>> handleApiException(final HttpServletRequest request,
                                                                      final Exception exception) {
        log.error(exception.getMessage(), exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request);
        int status = error.getStatus();
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            return ResponseEntity
                .status(error.getStatus())
                .build();
        } else {
            return ResponseEntity
                .status(error.getStatus())
                .body(error);
        }
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageConversionException.class
    })
    @ResponseBody
    public ResponseEntity<HttpError<Serializable>> handleCommonExceptionsAsBadRequest(final HttpServletRequest request,
                                                                                      final Exception exception) {
        log.error(exception.getMessage(), exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request, HttpStatus.BAD_REQUEST);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, 
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.debug("MethodArgumentNotValidException:{}", ex.getLocalizedMessage());
        final HttpError<Serializable> error = new HttpError<>(ex, request, HttpStatus.BAD_REQUEST)
            .withMessage(BAD_SCHEMA);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.debug("HttpMessageNotReadableException:{}", exception.getLocalizedMessage());
        final HttpError<Serializable> error = new HttpError<>(exception, request, HttpStatus.BAD_REQUEST)
            .withMessage(BAD_SCHEMA);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
    
}
