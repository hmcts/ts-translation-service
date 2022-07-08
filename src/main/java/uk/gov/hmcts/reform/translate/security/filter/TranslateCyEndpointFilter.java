package uk.gov.hmcts.reform.translate.security.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.reform.translate.model.ControllerConstants;
import uk.gov.hmcts.reform.translate.security.CustomPermitAllAuthenticationTokenBuilder;
import uk.gov.hmcts.reform.translate.security.HttpServletRequestWithoutAuthenticationHeader;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class TranslateCyEndpointFilter extends OncePerRequestFilter {

    private static final String POST_METHOD = "POST";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPostTranslateCyEndpoint(request)) {
            final AbstractAuthenticationToken authenticationToken =
                new CustomPermitAllAuthenticationTokenBuilder(request)
                    .build();

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(new HttpServletRequestWithoutAuthenticationHeader(request), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPostTranslateCyEndpoint(final HttpServletRequest request) {
        return POST_METHOD.equalsIgnoreCase(request.getMethod())
            && ControllerConstants.TRANSLATIONS_URL.equalsIgnoreCase(request.getServletPath());
    }

}
