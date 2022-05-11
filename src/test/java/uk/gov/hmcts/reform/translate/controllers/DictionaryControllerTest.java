package uk.gov.hmcts.reform.translate.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.translate.model.TranslationsRequest;
import uk.gov.hmcts.reform.translate.model.TranslationsResponse;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DictionaryController.class)
@AutoConfigureMockMvc(addFilters = false)
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
}
