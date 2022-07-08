package uk.gov.hmcts.reform.translate.security;

import org.springframework.http.HttpHeaders;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import static java.util.Collections.emptyList;

public class HttpServletRequestWithoutAuthenticationHeader extends HttpServletRequestWrapper {
    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request the {@link HttpServletRequest} to be wrapped.
     * @throws IllegalArgumentException if the request is null
     */
    public HttpServletRequestWithoutAuthenticationHeader(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getHeader(String name) {
        return isNotAuthorizationHeader(name)
            ? super.getHeader(name)
            : null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        final List<String> names = Collections.list(super.getHeaderNames());
        final List<String> filteredNames = names.stream()
            .filter(this::isNotAuthorizationHeader)
            .toList();

        return Collections.enumeration(filteredNames);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return isNotAuthorizationHeader(name)
            ? super.getHeaders(name)
            : Collections.enumeration(emptyList());
    }

    private boolean isNotAuthorizationHeader(final String name) {
        return !name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION);
    }
}
