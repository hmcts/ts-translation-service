package uk.gov.hmcts.reform.translate.wiremock.config;


import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.translate.wiremock.extensions.CustomisedResponseTransformer;
import uk.gov.hmcts.reform.translate.wiremock.extensions.DynamicOAuthJwkSetResponseTransformer;

@Configuration
public class WireMockTestConfiguration {

    @Bean
    public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
        return config -> config.extensions(new CustomisedResponseTransformer(),
                                           new DynamicOAuthJwkSetResponseTransformer());
    }
}
