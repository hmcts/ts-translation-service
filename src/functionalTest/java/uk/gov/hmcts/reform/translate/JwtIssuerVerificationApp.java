package uk.gov.hmcts.reform.translate;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.TestAutomationConfig;
import uk.gov.hmcts.befta.data.UserData;
import uk.gov.hmcts.befta.util.EnvironmentVariableUtils;
import uk.gov.hmcts.reform.translate.security.OidcIssuerConfiguration;

import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
public final class JwtIssuerVerificationApp {

    private JwtIssuerVerificationApp() {
    }

    public static void main(String[] args) {
        Set<String> expectedIssuers = OidcIssuerConfiguration.allowedIssuers(
            EnvironmentVariableUtils.getRequiredVariable("OIDC_ISSUER"),
            System.getenv("OIDC_ALLOWED_ISSUERS")
        );
        String actualIssuer = resolveIssuerFromRealToken();

        if (!expectedIssuers.contains(actualIssuer)) {
            throw new IllegalStateException(
                "OIDC issuer mismatch: expected one of `" + String.join("`, `", expectedIssuers)
                    + "` but token iss was `"
                    + actualIssuer + "`"
            );
        }

        log.info("Verified configured OIDC issuer matches functional test token iss: {}", actualIssuer);
    }

    private static String resolveIssuerFromRealToken() {
        BeftaMain.setConfig(TestAutomationConfig.INSTANCE);
        TranslationServiceTestAutomationAdapter adapter = new TranslationServiceTestAutomationAdapter();
        BeftaMain.setTaAdapter(adapter);

        try {
            UserData manageTranslationUser = TranslationServiceTestDataLoader.loadManageTranslationUser();
            adapter.authenticate(
                manageTranslationUser,
                BeftaMain.getConfig().getUserTokenProviderConfig().getClientId()
            );
            return issuerFrom(manageTranslationUser.getAccessToken());
        } catch (ExecutionException exception) {
            throw new IllegalStateException(
                "Failed to get a real functional test token for issuer verification",
                exception
            );
        }
    }

    private static String issuerFrom(String accessToken) {
        try {
            String issuer = SignedJWT.parse(accessToken).getJWTClaimsSet().getIssuer();
            if (StringUtils.isBlank(issuer)) {
                throw new IllegalStateException("Decoded IDAM access token did not contain an iss claim");
            }
            return issuer;
        } catch (ParseException exception) {
            throw new IllegalStateException("Failed to parse IDAM access token as a JWT", exception);
        }
    }
}
