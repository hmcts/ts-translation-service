package uk.gov.hmcts.reform.translate.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcIssuerConfigurationTest {

    @Test
    void shouldOnlyHavePrimaryIssuerWhenAllowedIssuersUnset() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", null))
            .containsExactly("primary");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void shouldOnlyHavePrimaryIssuerWhenAllowedIssuersBlank(String allowedIssuers) {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", allowedIssuers))
            .containsExactly("primary");
    }

    @Test
    void shouldIncludePrimaryAndConfiguredAllowedIssuers() {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", " secondary, tertiary , secondary "))
            .containsExactly("primary", "secondary", "tertiary");
    }

    @ParameterizedTest
    @MethodSource("allowedIssuersWithBlankEntries")
    void shouldIgnoreBlankEntriesInAllowedIssuers(String allowedIssuers, String[] expectedIssuers) {
        assertThat(OidcIssuerConfiguration.allowedIssuers("primary", allowedIssuers))
            .containsExactly(expectedIssuers);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectBlankPrimaryIssuerEvenWhenAllowedIssuersAreConfigured(String primaryIssuer) {
        assertThatThrownBy(() -> OidcIssuerConfiguration.allowedIssuers(primaryIssuer, "secondary"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("oidc.issuer must not be blank");
    }

    private static Stream<Arguments> allowedIssuersWithBlankEntries() {
        return Stream.of(
            Arguments.of(", secondary", new String[] {"primary", "secondary"}),
            Arguments.of("secondary,", new String[] {"primary", "secondary"}),
            Arguments.of("secondary,,tertiary", new String[] {"primary", "secondary", "tertiary"})
        );
    }
}
