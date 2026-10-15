package uk.gov.hmcts.reform.translate.errorhandling;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.translate.security.ErrorCode;
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
        final HttpError<Serializable> error = new HttpError<>(exception, request);
        logFailure(exception, error.getStatus());
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
        final HttpError<Serializable> error = new HttpError<>(exception, request, HttpStatus.BAD_REQUEST);
        logFailure(exception, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, 
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        final HttpError<Serializable> error = new HttpError<>(ex, request, HttpStatus.BAD_REQUEST)
            .withMessage(BAD_SCHEMA);
        logFailure(ex, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        final HttpError<Serializable> error = new HttpError<>(exception, request, HttpStatus.BAD_REQUEST)
            .withMessage(BAD_SCHEMA);
        logFailure(exception, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<HttpError<Serializable>> handleUnexpectedException(HttpServletRequest request,
                                                                            Exception exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (exception instanceof org.springframework.security.access.AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
        } else if (exception instanceof org.springframework.security.core.AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
        }
        logFailure(exception, status.value());
        return ResponseEntity.status(status).body(new HttpError<>(exception, request, status));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
                                                              HttpHeaders headers, HttpStatusCode status,
                                                              WebRequest request) {
        // Never forward Spring ProblemDetail text, rejected values or parser diagnostics.
        final HttpError<Serializable> error = new HttpError<>(exception, request,
            HttpStatus.valueOf(status.value()));
        logFailure(exception, status.value());
        return super.handleExceptionInternal(exception, error, headers, status, request);
    }

    private void logFailure(Exception exception, int status) {
        log.error("Request failed: code={} status={}", ErrorCode.fromStatus(status), status);
        log.debug("Original request failure: code={} status={}", ErrorCode.fromStatus(status), status, exception);
    }

}
