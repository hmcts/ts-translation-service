package uk.gov.hmcts.reform.translate.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcIssuerConfigurationTest {

    private static final String PRIMARY_ISSUER = "https://primary-issuer.example.com/o";
    private static final String SECONDARY_ISSUER = "https://secondary-issuer.example.com/o";
    private static final String TERTIARY_ISSUER = "https://tertiary-issuer.example.com/o";

    @Test
    void shouldOnlyHavePrimaryIssuerWhenAllowedIssuersUnset() {
        assertThat(OidcIssuerConfiguration.allowedIssuers(PRIMARY_ISSUER, null))
            .containsExactly(PRIMARY_ISSUER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void shouldOnlyHavePrimaryIssuerWhenAllowedIssuersBlank(String allowedIssuers) {
        assertThat(OidcIssuerConfiguration.allowedIssuers(PRIMARY_ISSUER, allowedIssuers))
            .containsExactly(PRIMARY_ISSUER);
    }

    @Test
    void shouldIncludePrimaryAndConfiguredAllowedIssuers() {
        assertThat(OidcIssuerConfiguration.allowedIssuers(
            PRIMARY_ISSUER,
            " " + SECONDARY_ISSUER + ", " + TERTIARY_ISSUER + " , " + SECONDARY_ISSUER + " "
        ))
            .containsExactly(PRIMARY_ISSUER, SECONDARY_ISSUER, TERTIARY_ISSUER);
    }

    @ParameterizedTest
    @ValueSource(strings = {", " + SECONDARY_ISSUER, SECONDARY_ISSUER + ","})
    void shouldIgnoreBlankEntryBeforeOrAfterAllowedIssuer(String allowedIssuers) {
        assertThat(OidcIssuerConfiguration.allowedIssuers(PRIMARY_ISSUER, allowedIssuers))
            .containsExactly(PRIMARY_ISSUER, SECONDARY_ISSUER);
    }

    @Test
    void shouldIgnoreBlankEntryBetweenAllowedIssuers() {
        assertThat(OidcIssuerConfiguration.allowedIssuers(
            PRIMARY_ISSUER,
            SECONDARY_ISSUER + ",," + TERTIARY_ISSUER
        ))
            .containsExactly(PRIMARY_ISSUER, SECONDARY_ISSUER, TERTIARY_ISSUER);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectBlankPrimaryIssuerEvenWhenAllowedIssuersAreConfigured(String primaryIssuer) {
        assertThatThrownBy(() -> OidcIssuerConfiguration.allowedIssuers(primaryIssuer, SECONDARY_ISSUER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("oidc.issuer must not be blank");
    }
}
