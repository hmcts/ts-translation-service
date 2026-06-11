package uk.gov.hmcts.reform.translate.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = "oidc.allowed-issuers=")
class JwtDecoderSingleIssuerValidationIT extends JwtDecoderIssuerValidationTestSupport {

    private static final String ADDITIONAL_ALLOWED_ISSUER = "http://additional-issuer";

    @Test
    void shouldAcceptTokenFromConfiguredIssuerWhenAllowedIssuersIsBlank() throws Exception {
        assertThat(jwtDecoder.decode(signedToken(issuer())).getIssuer().toString()).isEqualTo(issuer());
    }

    @Test
    void shouldRejectAdditionalIssuerWhenAllowedIssuersIsBlank() throws Exception {
        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(signedToken(ADDITIONAL_ALLOWED_ISSUER))
        );

        assertThat(exception.getMessage()).contains("iss");
    }
}
