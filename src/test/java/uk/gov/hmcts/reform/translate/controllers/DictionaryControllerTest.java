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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.translate.TestIdamConfiguration;
import uk.gov.hmcts.reform.translate.config.SecurityConfiguration;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

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

    private static final String CLIENTS2S_TOKEN = "clientS2SToken";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockBean
    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Nested
    @DisplayName("getDictionary")
    class GetDictionary {
        @Test
        void shouldReturn200() {
            DictionaryController dictionaryController = new DictionaryController(dictionaryService);
            dictionaryController.getDictionary();
            verify(dictionaryService).getDictionaryContents();
        }
    }


    @Nested
    @DisplayName("putDictionary")
    class PutDictionary {
        @Test
        void shouldReturn200() {
            final var dictionaryController = new DictionaryController(dictionaryService);
            final var getDictionaryRequest = getDictionaryRequest(1, 2);
            dictionaryController.putDictionary(getDictionaryRequest,"validClient");
            verify(dictionaryService, times(1)).putDictionary(getDictionaryRequest, CLIENTS2S_TOKEN);
        }

        private Dictionary getDictionaryRequest(int from, int to) {
            final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
            IntStream.range(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, "translated_" + i));
            return new Dictionary(expectedMapKeysAndValues);
        }
    }
}
