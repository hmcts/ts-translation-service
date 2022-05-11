package uk.gov.hmcts.reform.translate;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.translate.config.TestIdamConfiguration;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    Application.class,
    TestIdamConfiguration.class
})
@ActiveProfiles("itest")
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@SuppressWarnings("HideUtilityClassConstructor")
public class BaseTest {

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    @Autowired
    protected SecurityUtils securityUtils;

    @Mock
    protected Authentication authentication;

    protected static final String GET_TRANSLATION_TABLES_SCRIPT =
        "classpath:sql/get-Dictionary_And_TranslationUploads.sql";

    protected static final String DELETE_TRANSLATION_TABLES_SCRIPT =
        "classpath:sql/delete-Dictionary_And_TranslationUploads.sql";

    @BeforeEach
    void init() {
        Jwt jwt = dummyJwt();
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    @Configuration
    static class WireMockTestConfiguration {
        @Bean
        public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
            return config -> config.extensions(new WiremockFixtures.ConnectionClosedTransformer());
        }
    }

    private Jwt dummyJwt() {
        return Jwt.withTokenValue("a dummy jwt token")
            .claim("aClaim", "aClaim")
            .header("aHeader", "aHeader")
            .build();
    }
}
