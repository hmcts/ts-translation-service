package uk.gov.hmcts.reform.translate.security.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.reform.auth.checker.core.service.ServiceRequestAuthorizer;
import uk.gov.hmcts.reform.translate.model.ControllerConstants;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;
import uk.gov.hmcts.reform.translate.security.SpecialAuthenticationTokenBuilder;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class SpecialPermitFilter extends OncePerRequestFilter {

    private final SecurityUtils securityUtils;

    private static final String PUT_METHOD = "PUT";

    @Autowired
    public SpecialPermitFilter(final SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isPutDictionaryEndpoint(request) && isDefinitionStore(request)) {
            final AbstractAuthenticationToken authenticationToken = new SpecialAuthenticationTokenBuilder(request)
                .build();

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPutDictionaryEndpoint(final HttpServletRequest request) {
        return PUT_METHOD.equalsIgnoreCase(request.getMethod())
            && ControllerConstants.DICTIONARY_URL.equalsIgnoreCase(request.getServletPath());
    }

    private boolean isDefinitionStore(final HttpServletRequest request) {
        final String bearerToken = extractBearerToken(request);
        final String serviceName = securityUtils.getServiceNameFromS2SToken(bearerToken);

        return securityUtils.isBypassAuthCheck(serviceName);
    }

    private String extractBearerToken(HttpServletRequest request) {
        final String token = request.getHeader(ServiceRequestAuthorizer.AUTHORISATION);

        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }

}
