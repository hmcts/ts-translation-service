package uk.gov.hmcts.reform.translate.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.translate.TestIdamConfiguration;
import uk.gov.hmcts.reform.translate.config.SecurityConfiguration;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.model.TranslationsRequest;
import uk.gov.hmcts.reform.translate.model.TranslationsResponse;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DictionaryController.class,
    excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes =
        {SecurityConfiguration.class, JwtGrantedAuthoritiesConverter.class}))
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(TestIdamConfiguration.class)
class DictionaryControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockBean
    private DictionaryService dictionaryService;

    private DictionaryController dictionaryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        dictionaryController = new DictionaryController(dictionaryService);
    }

    @Nested
    @DisplayName("getDictionary")
    class GetDictionary {
        @Test
        void shouldReturn200() {
            dictionaryController.getDictionary();
            verify(dictionaryService).getDictionaryContents();
        }
    }

    @Nested
    @DisplayName("GetTranslation")
    class GetTranslations {
        @Test
        void shouldReturnTranslations() {
            doReturn(Map.of("English phrase", "Translated English phrase"))
                .when(dictionaryService).getTranslations(anyList());

            final TranslationsRequest translationRequest = new TranslationsRequest(List.of("English phrase"));
            final ResponseEntity<TranslationsResponse> responseEntity =
                dictionaryController.getTranslation(translationRequest);

            assertThat(responseEntity)
                .isNotNull()
                .satisfies(response -> assertThat(response.getBody()).isNotNull());
            verify(dictionaryService).getTranslations(anyList());
        }
    }


    @Nested
    @DisplayName("putDictionary")
    class PutDictionary {
        @Test
        void shouldReturn200() {
            final var dictionaryController = new DictionaryController(dictionaryService);
            final var getDictionaryRequest = getDictionaryRequest(1, 2);
            dictionaryController.putDictionary(getDictionaryRequest);
            verify(dictionaryService, times(1)).putDictionary(getDictionaryRequest);
        }

        private Dictionary getDictionaryRequest(int from, int to) {
            final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
            IntStream.range(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, "translated_" + i));
            return new Dictionary(expectedMapKeysAndValues);
        }
    }
}
