package uk.gov.hmcts.reform.translate.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.security.filter.PutDictionaryEndpointFilter;
import uk.gov.hmcts.reform.translate.security.filter.TranslateCyEndpointFilter;
import uk.gov.hmcts.reform.translate.util.KeyGenerator;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JwtDecoderIssuerValidationIT {

    private static final String ADDITIONAL_ALLOWED_ISSUER = "http://additional-issuer";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() throws JOSEException {
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        String issuer = issuer();
        wireMockServer.stubFor(
            get(urlEqualTo("/o/.well-known/openid-configuration"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "/jwks\"}")
                )
        );
        wireMockServer.stubFor(
            get(urlEqualTo("/o/jwks"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[" + KeyGenerator.getRsaJwk().toPublicJWK().toJSONString() + "]}")
                )
        );
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldAcceptTokenFromConfiguredIssuer() throws Exception {
        JwtDecoder jwtDecoder = jwtDecoder();

        assertThat(jwtDecoder.decode(signedToken(issuer())).getIssuer().toString()).isEqualTo(issuer());
    }

    @Test
    void shouldRejectTokenFromUnexpectedIssuer() throws Exception {
        JwtDecoder jwtDecoder = jwtDecoder();

        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(signedToken(INVALID_ISSUER))
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    @Test
    void shouldAcceptTokenFromAdditionalAllowedIssuerWhenConfigured() throws Exception {
        JwtDecoder jwtDecoder = jwtDecoder(ADDITIONAL_ALLOWED_ISSUER);

        assertThat(jwtDecoder.decode(signedToken(ADDITIONAL_ALLOWED_ISSUER)).getIssuer().toString())
            .isEqualTo(ADDITIONAL_ALLOWED_ISSUER);
    }

    @Test
    void shouldRejectExpiredTokenEvenWhenIssuerMatches() throws Exception {
        JwtDecoder jwtDecoder = jwtDecoder();

        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(signedToken(issuer(), Instant.now().minusSeconds(60)))
        );

        assertThat(exception.getMessage()).contains("Jwt expired");
    }

    private JwtDecoder jwtDecoder() {
        return jwtDecoder(null);
    }

    private JwtDecoder jwtDecoder(String allowedIssuers) {
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(
            mock(uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter.class),
            mock(PutDictionaryEndpointFilter.class),
            mock(TranslateCyEndpointFilter.class),
            mock(JwtGrantedAuthoritiesConverter.class)
        );
        ReflectionTestUtils.setField(securityConfiguration, "issuerUri", issuer());
        ReflectionTestUtils.setField(securityConfiguration, "issuerOverride", issuer());
        ReflectionTestUtils.setField(securityConfiguration, "allowedIssuers", allowedIssuers);
        return securityConfiguration.jwtDecoder();
    }

    private String issuer() {
        return wireMockServer.baseUrl() + "/o";
    }

    private String signedToken(String issuer) throws JOSEException, ParseException {
        return signedToken(issuer, Instant.now().plusSeconds(300));
    }

    private String signedToken(String issuer, Instant expiresAt) throws JOSEException, ParseException {
        Instant issuedAt = expiresAt.isBefore(Instant.now())
            ? expiresAt.minusSeconds(60)
            : Instant.now().minusSeconds(60);
        SignedJWT signedJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(KeyGenerator.getRsaJwk().getKeyID())
                .type(JOSEObjectType.JWT)
                .build(),
            new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("user")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build()
        );
        signedJwt.sign(new RSASSASigner(KeyGenerator.getRsaJwk().toPrivateKey()));
        return signedJwt.serialize();
    }
}
