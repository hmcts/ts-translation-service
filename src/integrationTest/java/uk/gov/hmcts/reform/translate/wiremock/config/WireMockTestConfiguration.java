package uk.gov.hmcts.reform.translate.wiremock.config;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Configuration;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.WireMockConfigurationCustomizer;
import uk.gov.hmcts.reform.translate.wiremock.extensions.CustomisedResponseTransformer;
import uk.gov.hmcts.reform.translate.wiremock.extensions.DynamicOAuthJwkSetResponseTransformer;

@Configuration
public class WireMockTestConfiguration implements WireMockConfigurationCustomizer {

    @Override
    public void customize(WireMockConfiguration config, ConfigureWireMock configureWireMock) {
        config.extensions(
            new CustomisedResponseTransformer(),
            new DynamicOAuthJwkSetResponseTransformer()
        );
    }
}
