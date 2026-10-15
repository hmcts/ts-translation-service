package uk.gov.hmcts.reform.translate.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Keeps authentication diagnostics in server logs, not in the Bearer challenge. */
@Slf4j
public class SafeAuthenticationErrors implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        if (exception instanceof OAuth2AuthenticationException oauthException
            && oauthException.getError() instanceof BearerTokenError bearerError) {
            status = bearerError.getHttpStatus();
        }
        ErrorCode code = ErrorCode.fromStatus(status.value());
        log.warn("Authentication failed: code={} status={}", code, status.value());
        log.debug("Original authentication failure: code={} status={}", code, status.value(), exception);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setStatus(status.value());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        log.warn("Access denied: code={} status=403", ErrorCode.ACCESS_DENIED);
        log.debug("Original access denial: code={} status=403", ErrorCode.ACCESS_DENIED, exception);
        response.setStatus(HttpStatus.FORBIDDEN.value());
    }
}
