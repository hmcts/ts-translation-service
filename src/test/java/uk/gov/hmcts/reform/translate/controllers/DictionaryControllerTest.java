package uk.gov.hmcts.reform.translate.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import uk.gov.hmcts.reform.translate.config.SecurityConfiguration;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.model.Translation;
import uk.gov.hmcts.reform.translate.model.TranslationsRequest;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.security.filter.PutDictionaryEndpointFilter;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

@WebMvcTest(controllers = DictionaryController.class,
    excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE,
        classes = {PutDictionaryEndpointFilter.class, SecurityConfiguration.class,
            JwtGrantedAuthoritiesConverter.class}))
class DictionaryControllerTest extends BaseControllerTest {

    private static final String CLIENTS2S_TOKEN = "clientS2SToken";

    @MockitoBean
    private DictionaryService dictionaryService;

    private DictionaryController dictionaryController;

    @BeforeEach
    public void setUp() {
        dictionaryController = new DictionaryController(dictionaryService);
    }

    @Nested
    @DisplayName("getDictionary")
    protected class GetDictionary {
        @Test
        void shouldReturn200() {
            dictionaryController.getDictionary();
            verify(dictionaryService).getDictionaryContents();
        }
    }

    @Nested
    @DisplayName("RequestTranslation")
    protected class RequestTranslations {
        @Test
        void shouldReturnTranslations() {
            doReturn(Map.of("English phrase", "Translated English phrase"))
                .when(dictionaryService).getTranslations(anySet());

            final TranslationsRequest translationRequest = new TranslationsRequest(Set.of("English phrase"));
            final Dictionary dictionary = dictionaryController.getTranslation(translationRequest);

            assertThat(dictionary)
                .isNotNull();
            verify(dictionaryService).getTranslations(anySet());
        }
    }

    @Nested
    @DisplayName("putDictionary")
    protected class PutDictionary {
        @Test
        void shouldReturn200() {
            final var dictionaryController = new DictionaryController(dictionaryService);
            final var getDictionaryRequest = getDictionaryRequest(1, 2);
            dictionaryController.putDictionary(getDictionaryRequest, CLIENTS2S_TOKEN);
            verify(dictionaryService, times(1)).putDictionary(getDictionaryRequest);
        }

        private Dictionary getDictionaryRequest(int from, int to) {
            final Map<String, Translation> expectedMap = new HashMap<>();
            IntStream.range(from, to).forEach(i -> expectedMap.put("english_" + i, new Translation("translated_" + i)));
            return new Dictionary(expectedMap);
        }
    }
}
