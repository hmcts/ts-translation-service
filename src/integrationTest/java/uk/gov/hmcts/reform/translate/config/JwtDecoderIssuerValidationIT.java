package uk.gov.hmcts.reform.translate.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = "oidc.allowed-issuers=http://additional-issuer")
class JwtDecoderIssuerValidationIT extends JwtDecoderIssuerValidationTestSupport {

    private static final String ADDITIONAL_ALLOWED_ISSUER = "http://additional-issuer";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";

    @Test
    void shouldAcceptTokenFromConfiguredIssuer() throws Exception {
        assertThat(jwtDecoder.decode(signedToken(issuer())).getIssuer().toString()).isEqualTo(issuer());
    }

    @Test
    void shouldRejectTokenFromUnexpectedIssuer() throws Exception {
        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(signedToken(INVALID_ISSUER))
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldAcceptTokenFromAdditionalAllowedIssuerWhenConfigured() throws Exception {
        assertThat(jwtDecoder.decode(signedToken(ADDITIONAL_ALLOWED_ISSUER)).getIssuer().toString())
            .isEqualTo(ADDITIONAL_ALLOWED_ISSUER);
    }

    @Test
    void shouldRejectTokenWithoutIssuer() throws Exception {
        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(signedTokenWithoutIssuer())
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldRejectExpiredTokenEvenWhenIssuerMatches() throws Exception {
        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(signedToken(issuer(), Instant.now().minusSeconds(60)))
        );

        assertThat(exception.getMessage()).contains("Jwt expired");
    }
}
