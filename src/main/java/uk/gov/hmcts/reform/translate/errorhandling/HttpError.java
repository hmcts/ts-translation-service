package uk.gov.hmcts.reform.translate.errorhandling;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.servlet.http.HttpServletRequest;

@Getter
public class HttpError<T extends Serializable> implements Serializable {

    public static final Integer DEFAULT_STATUS = HttpStatus.INTERNAL_SERVER_ERROR.value();
    public static final String DEFAULT_ERROR = "Unexpected Error";

    private final String exception;
    private final transient LocalDateTime timestamp;
    private final Integer status;
    private final String error;
    private String message;
    private final String path;

    public HttpError(Exception exception, String path, HttpStatus status) {
        final var responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);

        this.exception = exception.getClass().getName();
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
        this.status = getStatusFromResponseStatus(responseStatus, status);
        this.error = getErrorReason(responseStatus, status);
        this.message = exception.getMessage();
        this.path = UriUtils.encodePath(path, StandardCharsets.UTF_8);
    }

    public HttpError(Exception exception, HttpServletRequest request, HttpStatus status) {
        this(exception, request.getRequestURI(), status);
    }

    public HttpError(Exception exception, WebRequest request, HttpStatus status) {
        this(exception, ((ServletWebRequest)request).getRequest().getRequestURI(), status);
    }

    public HttpError(Exception exception, HttpServletRequest request) {
        this(exception, request, null);
    }

    private Integer getStatusFromResponseStatus(ResponseStatus responseStatus, HttpStatus status) {
        if (status != null) {
            return status.value();
        }
        if (null != responseStatus) {
            final var httpStatus = getHttpStatus(responseStatus);
            if (null != httpStatus) {
                return httpStatus.value();
            }
        }

        return DEFAULT_STATUS;
    }

    private HttpStatus getHttpStatus(ResponseStatus responseStatus) {
        if (!HttpStatus.INTERNAL_SERVER_ERROR.equals(responseStatus.value())) {
            return responseStatus.value();
        } else if (!HttpStatus.INTERNAL_SERVER_ERROR.equals(responseStatus.code())) {
            return responseStatus.code();
        }

        return null;
    }

    private String getErrorReason(ResponseStatus responseStatus, HttpStatus status) {
        if (null != responseStatus) {
            if (!responseStatus.reason().isEmpty()) {
                return responseStatus.reason();
            }

            final var httpStatus = getHttpStatus(responseStatus);
            if (null != httpStatus) {
                return httpStatus.getReasonPhrase();
            }
        } else if (null != status) {
            return status.getReasonPhrase();
        }

        return DEFAULT_ERROR;
    }

    public HttpError<T> withMessage(String message) {
        this.message = message;
        return this;
    }

}
