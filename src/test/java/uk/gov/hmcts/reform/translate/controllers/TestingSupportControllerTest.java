package uk.gov.hmcts.reform.translate.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import uk.gov.hmcts.reform.translate.config.SecurityConfiguration;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import static org.mockito.Mockito.verify;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

@WebMvcTest(
    controllers = TestingSupportController.class,
    properties = {"ts.endpoints.testing-support.enabled=true"},
    excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE,
        classes = {SecurityConfiguration.class, JwtGrantedAuthoritiesConverter.class})
)
class TestingSupportControllerTest extends BaseControllerTest {

    @MockBean
    private DictionaryService dictionaryService;

    private TestingSupportController endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new TestingSupportController(dictionaryService);
    }

    @Nested
    @DisplayName("DeleteDictionaryTestPhrases")
    class DeleteDictionaryTestPhrases {

        @Test
        void shouldCallDeleteTestPhrasesService() {

            // WHEN
            endpoint.deleteDictionaryTestPhrases();

            // THEN
            verify(dictionaryService).deleteTestPhrases();

        }

    }

}
