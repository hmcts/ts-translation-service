package uk.gov.hmcts.reform.translate.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import jakarta.servlet.http.HttpServletRequest;

import static java.util.Collections.emptyList;

public record CustomPermitAllAuthenticationTokenBuilder(HttpServletRequest request) {

    public AbstractAuthenticationToken build() {
        AbstractAuthenticationToken authenticationToken = new AbstractAuthenticationToken(emptyList()) {
            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return null;
            }
        };
        authenticationToken.setAuthenticated(true);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        return authenticationToken;
    }
}
