package uk.gov.hmcts.reform.translate.security.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.reform.translate.model.ControllerConstants;
import uk.gov.hmcts.reform.translate.security.CustomPermitAllAuthenticationTokenBuilder;
import uk.gov.hmcts.reform.translate.security.HttpServletRequestWithoutAuthenticationHeader;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static uk.gov.hmcts.reform.translate.security.SecurityUtils.SERVICE_AUTHORIZATION;

@Slf4j
@Component
public class PutDictionaryEndpointFilter extends OncePerRequestFilter {

    private final SecurityUtils securityUtils;

    @Autowired
    public PutDictionaryEndpointFilter(final SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPutDictionaryEndpoint(request) && isPermittedService(request)) {
            final AbstractAuthenticationToken authenticationToken =
                new CustomPermitAllAuthenticationTokenBuilder(request)
                    .build();

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(new HttpServletRequestWithoutAuthenticationHeader(request), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPutDictionaryEndpoint(final HttpServletRequest request) {
        return HttpMethod.PUT.name().equalsIgnoreCase(request.getMethod())
            && ControllerConstants.DICTIONARY_URL.equalsIgnoreCase(request.getServletPath());
    }

    private boolean isPermittedService(final HttpServletRequest request) {
        final String serviceName = securityUtils.getServiceNameFromS2SToken(request.getHeader(SERVICE_AUTHORIZATION));

        return securityUtils.isBypassAuthCheck(serviceName);
    }

}
