package uk.gov.hmcts.reform.translate.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import uk.gov.hmcts.reform.translate.BaseTest;
import uk.gov.hmcts.reform.translate.util.KeyGenerator;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

abstract class JwtDecoderIssuerValidationTestSupport extends BaseTest {

    @Autowired
    protected JwtDecoder jwtDecoder;

    protected String issuer() {
        return "http://localhost:" + wiremockPort + "/o";
    }

    protected String signedToken(String issuer) throws JOSEException, ParseException {
        return signedToken(issuer, Instant.now().plusSeconds(300));
    }

    protected String signedToken(String issuer, Instant expiresAt) throws JOSEException, ParseException {
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

    protected String signedTokenWithoutIssuer() throws JOSEException, ParseException {
        Instant issuedAt = Instant.now().minusSeconds(60);
        Instant expiresAt = Instant.now().plusSeconds(300);
        SignedJWT signedJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(KeyGenerator.getRsaJwk().getKeyID())
                .type(JOSEObjectType.JWT)
                .build(),
            new JWTClaimsSet.Builder()
                .subject("user")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build()
        );
        signedJwt.sign(new RSASSASigner(KeyGenerator.getRsaJwk().toPrivateKey()));
        return signedJwt.serialize();
    }
}
