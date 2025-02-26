package uk.gov.hmcts.reform.translate.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import uk.gov.hmcts.reform.translate.config.SecurityConfiguration;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.security.filter.PutDictionaryEndpointFilter;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import static org.mockito.Mockito.verify;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

@WebMvcTest(
    controllers = TestingSupportController.class,
    properties = {"ts.endpoints.testing-support.enabled=true"},
    excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE,
        classes = {PutDictionaryEndpointFilter.class, SecurityConfiguration.class,
            JwtGrantedAuthoritiesConverter.class})
)
class TestingSupportControllerTest extends BaseControllerTest {

    @MockitoBean
    private DictionaryService dictionaryService;

    private TestingSupportController endpoint;

    @BeforeEach
    public void setUp() {
        endpoint = new TestingSupportController(dictionaryService);
    }

    @Nested
    @DisplayName("DeleteDictionaryTestPhrases")
    protected class DeleteDictionaryTestPhrases {

        @Test
        void shouldCallDeleteTestPhrasesService() {

            // WHEN
            endpoint.deleteDictionaryTestPhrases();

            // THEN
            verify(dictionaryService).deleteTestPhrases();

        }

    }

}
