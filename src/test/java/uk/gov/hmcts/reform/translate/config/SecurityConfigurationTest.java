package uk.gov.hmcts.reform.translate.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigurationTest {

    private static final String VALID_ISSUER = "http://fr-am:8080/openam/oauth2/realms/root/realms/hmcts";
    private static final String SECONDARY_ISSUER = "http://idam-api:8080/o";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";

    @Test
    void shouldAcceptJwtFromConfiguredIssuerByDefault() {
        assertFalse(
            jwtValidator(null).validate(buildJwt(VALID_ISSUER, Instant.now().plusSeconds(300))).hasErrors()
        );
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuerByDefault() {
        assertTrue(
            jwtValidator(null).validate(buildJwt(INVALID_ISSUER, Instant.now().plusSeconds(300))).hasErrors()
        );
    }

    @Test
    void shouldAcceptJwtFromAdditionalAllowedIssuerWhenConfigured() {
        assertFalse(
            jwtValidator(SECONDARY_ISSUER)
                .validate(buildJwt(SECONDARY_ISSUER, Instant.now().plusSeconds(300)))
                .hasErrors()
        );
    }

    @Test
    void shouldAcceptConfiguredIssuerWhenAllowedIssuersAreConfigured() {
        assertFalse(
            jwtValidator(SECONDARY_ISSUER)
                .validate(buildJwt(VALID_ISSUER, Instant.now().plusSeconds(300)))
                .hasErrors()
        );
    }

    @Test
    void shouldRejectJwtOutsideConfiguredAllowedIssuers() {
        assertTrue(
            jwtValidator(SECONDARY_ISSUER)
                .validate(buildJwt(INVALID_ISSUER, Instant.now().plusSeconds(300)))
                .hasErrors()
        );
    }

    @Test
    void shouldUseConfiguredIssuerWhenAllowedIssuersIsBlank() {
        assertFalse(
            jwtValidator("   ").validate(buildJwt(VALID_ISSUER, Instant.now().plusSeconds(300))).hasErrors()
        );
    }

    @Test
    void shouldRejectJwtWithoutIssuerWhenMultipleIssuersConfigured() {
        assertTrue(
            jwtValidator(VALID_ISSUER + "," + SECONDARY_ISSUER)
                .validate(buildJwtWithoutIssuer(Instant.now().plusSeconds(300)))
                .hasErrors()
        );
    }

    @Test
    void shouldHandleBlankAndDuplicateAllowedIssuers() {
        assertFalse(
            jwtValidator(" , " + SECONDARY_ISSUER + ", " + SECONDARY_ISSUER + " , ")
                .validate(buildJwt(SECONDARY_ISSUER, Instant.now().plusSeconds(300)))
                .hasErrors()
        );
    }

    @Test
    void shouldFailFastWhenNoIssuerValuesAreConfigured() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> jwtValidator("   ", " , , ")
        );

        assertTrue(exception.getMessage().contains("At least one OIDC issuer must be configured"));
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenIssuerMatches() {
        assertTrue(
            jwtValidator(null).validate(buildJwt(VALID_ISSUER, Instant.now().minusSeconds(60))).hasErrors()
        );
    }

    private OAuth2TokenValidator<Jwt> jwtValidator(String allowedIssuers) {
        return jwtValidator(VALID_ISSUER, allowedIssuers);
    }

    private OAuth2TokenValidator<Jwt> jwtValidator(String issuerOverride, String allowedIssuers) {
        return SecurityConfiguration.jwtValidator(issuerOverride, allowedIssuers);
    }

    private Jwt buildJwt(String issuer, Instant expiresAt) {
        Instant issuedAt = expiresAt.isBefore(Instant.now())
            ? expiresAt.minusSeconds(60)
            : Instant.now().minusSeconds(60);
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer(issuer)
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }

    private Jwt buildJwtWithoutIssuer(Instant expiresAt) {
        Instant issuedAt = expiresAt.isBefore(Instant.now())
            ? expiresAt.minusSeconds(60)
            : Instant.now().minusSeconds(60);
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
