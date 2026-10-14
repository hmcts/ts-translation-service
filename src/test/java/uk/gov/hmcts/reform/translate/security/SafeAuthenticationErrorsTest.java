package uk.gov.hmcts.reform.translate.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class SafeAuthenticationErrorsTest {

    @Test
    void omitsAuthenticationDiagnosticsFromHeadersAndBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SafeAuthenticationErrors().commence(new MockHttpServletRequest(), response,
            new BadCredentialsException("Internal decoder at http://internal-service/jwks"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void preservesForbiddenWithoutDiagnostics() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SafeAuthenticationErrors().handle(new MockHttpServletRequest(), response,
            new AccessDeniedException("Internal access rule details"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEmpty();
    }
}
