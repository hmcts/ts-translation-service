package uk.gov.hmcts.reform.translate.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.NestedServletException;
import uk.gov.hmcts.reform.translate.BaseTest;
import uk.gov.hmcts.reform.translate.model.Dictionary;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DictionaryControllerIT extends BaseTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private static final String URL = "/dictionary";

    @Nested
    @DisplayName("Get Dictionary")
    @Transactional
    class GetDictionary {

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
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

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_DUPLICATE_ENGLISH_PHRASES_SCRIPT})
        void shouldThrowExceptionWhenDictionaryReturnsDuplicateEnglishPhrases() {
            NestedServletException nestedServletException = assertThrows(
                NestedServletException.class,
                () -> mockMvc.perform(get(URL)
                                          .contentType(MediaType.APPLICATION_JSON_VALUE))
            );

            assertTrue(nestedServletException.getCause() instanceof IllegalStateException);
            assertTrue(nestedServletException.getCause().getMessage().contains("Duplicate key English Phrase 1"));
        }

        @Test
        void shouldReturn403WhenUserDoesNotHaveManageTranslationsRole() throws Exception {
            stubUserInfo("unknown-role");
            mockMvc.perform(get(URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(403))
                .andReturn();
        }
    }

    @Nested
    @DisplayName("Put Dictionary")
    class PutDictionary {
        private static final String SERVICE_JWT_DEFINITION =
            "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZGVmaW5pdGlvbiIsImV4cCI6MTY1"
                + "NDEwNjYwNX0.aPkqfAzRIYI4yv7-J-JfkeUSTDTuxKSjRbe3dcMKGDo0HNMXz8IEIJGoYBx12SZUb7nzdd8siKIqNl5gmePOOQ";

        private static final String SERVICE_JWT_XUI_WEB =
            "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ4dWlfd2ViYXBwIiwiZXhwIjoxNjU0MTA4Mz"
                + "kyfQ.NUnKrHdqu_OBrb7MtdytKd6Yj3jlbz6DZuD_wkXBiOv5eQvm4CPTPZSDtAp00YqU-xJ5pVDI7dsslCe3_j4rNw";

        // manage-translations
        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamMUserWithManageTranslationCreateANewRecord() throws Exception {

            stubUserInfo("manage-translations");
            mockMvc.perform(put(URL)
                                .header("ServiceAuthorization", SERVICE_JWT_XUI_WEB)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(getDictionaryRequest(1, 2))))
                .andExpect(status().is(201))
                .andReturn();
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT,PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithManageTranslationUpdateARecord() throws Exception {
            stubUserInfo("manage-translations");
            mockMvc.perform(put(URL)
                                .header("ServiceAuthorization", SERVICE_JWT_XUI_WEB)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(getDictionaryRequest(1, 2))))
                .andExpect(status().is(201))
                .andReturn();
        }


        // load-translations user
        @Test
        void shouldReturn201ForPutDictionaryForIdamUserWithLoadTranslation() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(URL)
                                .header("ServiceAuthorization", SERVICE_JWT_XUI_WEB)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(getDictionaryRequestWithoutABody(1, 2))))
                .andExpect(status().is(201))
                .andReturn();

        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT,PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithLoadTranslationUpdateARecord() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(URL)
                                .header("ServiceAuthorization", SERVICE_JWT_XUI_WEB)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(getDictionaryRequestWithoutABody(1, 2))))
                .andExpect(status().is(201))
                .andReturn();
        }

        // 400 errors
        @Test
        void shouldReturn400ForPutDictionaryForIdamUserWithLoadTranslationWithIncorrectPayLoad() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(URL)
                                .header("ServiceAuthorization", SERVICE_JWT_XUI_WEB)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(getDictionaryRequest(1, 2))))
                .andExpect(status().is(400))
                .andReturn();
        }

        @Test
        void shouldReturn400ForPutDictionaryForIdamDefinitionStoreWithIncorrectPayLoad() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(URL)
                                .header("ServiceAuthorization", SERVICE_JWT_DEFINITION)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(getDictionaryRequest(1, 2))))
                .andExpect(status().is(400))
                .andReturn();
        }


        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn400ForPutDictionaryForNonIdam() throws Exception {
            mockMvc.perform(put(URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(getDictionaryRequest(1, 2))))
                .andExpect(status().is(400))
                .andReturn();
        }

    }

    private Dictionary getDictionaryRequest(int from, int to) {
        final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
        IntStream.range(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, "translated_" + i));
        return new Dictionary(expectedMapKeysAndValues);
    }

    private Dictionary getDictionaryRequestWithoutABody(int from, int to) {
        final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
        IntStream.range(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, null));
        return new Dictionary(expectedMapKeysAndValues);
    }

}
