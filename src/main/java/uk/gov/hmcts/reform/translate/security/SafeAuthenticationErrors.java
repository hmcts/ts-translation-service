package uk.gov.hmcts.reform.translate.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Keeps authentication diagnostics in server logs, not in the Bearer challenge. */
@Slf4j
public class SafeAuthenticationErrors implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) {
        log.warn("Authentication failed", exception);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        log.warn("Access denied", exception);
        response.setStatus(HttpStatus.FORBIDDEN.value());
    }
}
