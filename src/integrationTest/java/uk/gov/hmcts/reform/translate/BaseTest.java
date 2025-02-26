package uk.gov.hmcts.reform.translate;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.util.Date;

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
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings({"HideUtilityClassConstructor"})
public abstract class BaseTest {

    // data values as per: classpath:sql/add-Dictionary_TestPhrasesForDeletion.sql
    public static final String DELETE_ME_PHRASE_WITH_TRANSLATION_1 = "TEST-delete-me-with-translation-1";
    public static final String DELETE_ME_PHRASE_WITH_TRANSLATION_2 = "TEST-delete-me-with-translation-2";
    public static final String DELETE_ME_PHRASE_WITHOUT_TRANSLATION = "TEST-delete-me-no-translation";
    public static final String KEEP_ME_PHRASE_WITH_TRANSLATION_1 = "keep-me-with-translation-1";
    public static final String KEEP_ME_PHRASE_WITH_TRANSLATION_2 = "keep-me-with-translation-2";
    public static final String KEEP_ME_PHRASE_WITHOUT_TRANSLATION = "keep-me-no-translation";

    // data values as per: classpath:sql/get-Dictionary_And_TranslationUploads.sql
    public static final String GET_DICTIONARY_TEST_PHRASE_1 = "English Phrase 1";
    public static final String GET_DICTIONARY_TEST_PHRASE_2 = "English Phrase 2";
    public static final String GET_DICTIONARY_TEST_PHRASE_2_TRANSLATION = "Translated Phrase 2";
    public static final String GET_DICTIONARY_TEST_PHRASE_3_TRANSLATION = "Translated Phrase 1";

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

    protected static final String ADD_ENGLISH_PHRASE_SCRIPT = "classpath:sql/add-Dictionary.sql";

    protected static final String ADD_TEST_PHRASES_FOR_DELETION_SCRIPT =
        "classpath:sql/add-Dictionary_TestPhrasesForDeletion.sql";

    protected static final String PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT =
        "classpath:sql/put-create-Dictionary_EnglishPhrases.sql";

    @BeforeEach
    void init() {
        Jwt jwt = dummyJwt();
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    protected Jwt dummyJwt() {
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

    public static String generateDummyS2SToken(String serviceName) {
        return Jwts.builder()
            .setSubject(serviceName)
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS256, Keys.secretKeyFor(SignatureAlgorithm.HS256))
            .compact();
    }
}
