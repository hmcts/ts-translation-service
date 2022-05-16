package uk.gov.hmcts.reform.translate;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    Application.class,
    TestIdamConfiguration.class
})
@ActiveProfiles("itest")
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@SuppressWarnings({"HideUtilityClassConstructor", "PMD.LawOfDemeter", "PMD.JUnitAssertionsShouldIncludeMessage"})
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

    protected static final String GET_TRANSLATION_TABLES_DUPLICATE_ENGLISH_PHRASES_SCRIPT =
        "classpath:sql/get-Dictionary_WithDuplicateEnglishPhrases.sql";

    @BeforeEach
    void init() {
        Jwt jwt = dummyJwt();
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private Jwt dummyJwt() {
        return Jwt.withTokenValue("a dummy jwt token")
            .claim("aClaim", "aClaim")
            .header("aHeader", "aHeader")
            .build();
    }

    protected static void stubUserInfo(String roles) {
        final var jsonBody = "{\n"
            + "      \"sub\": \"user@hmcts.net\",\n"
            + "      \"uid\": \"e8275d41-7f22-4ee7-8ed3-14644d6db096\",\n"
            + "      \"roles\": [\n"
            + "        \"" + roles + "\"\n"
            + "      ],\n"
            + "      \"name\": \"Test User\",\n"
            + "      \"given_name\": \"Test\",\n"
            + "      \"family_name\": \"User\"\n"
            + "    }";
        stubFor(WireMock.get(urlEqualTo("/o/userinfo")).willReturn(
            aResponse().withStatus(HttpStatus.OK.value())
                .withHeader("Content-Type", "application/json")
                .withBody(jsonBody)));
    }
}
