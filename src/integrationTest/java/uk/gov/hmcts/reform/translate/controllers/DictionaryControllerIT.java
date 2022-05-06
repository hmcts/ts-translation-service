package uk.gov.hmcts.reform.translate.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.translate.BaseTest;

import java.util.Collections;
import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class DictionaryControllerIT extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String URL = "/dictionary";

    @Nested
    @DisplayName("Get Dictionary")
    class GetDictionary {

        @Test
        void shouldReturn200WhenDictionaryReturnsNoResults() throws Exception {
            mockMvc.perform(get(URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.translations", is(Collections.emptyMap())))
                .andReturn();
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn200WhenDictionaryReturnsResults() throws Exception {
            var expectedDictionary = new LinkedHashMap<String, String>();

            expectedDictionary.put("English Phrase 1", "");
            expectedDictionary.put("English Phrase 2", "Translated Phrase 2");
            expectedDictionary.put("English Phrase 3", "Translated Phrase 1");

            mockMvc.perform(get(URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.translations", equalTo(expectedDictionary)))
                .andReturn();
        }
    }
}
