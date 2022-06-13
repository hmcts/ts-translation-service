package uk.gov.hmcts.reform.translate.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
import uk.gov.hmcts.reform.translate.BaseTest;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DictionaryControllerIT extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected DictionaryRepository dictionaryRepository;

    private static final String DICTIONARY_URL = "/dictionary";

    @Nested
    @DisplayName("Get Dictionary")
    class GetDictionary {

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn200WhenDictionaryReturnsNoResults() throws Exception {
            mockMvc.perform(get(DICTIONARY_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.translations", is(emptyMap())))
                .andReturn();
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn200WhenDictionaryReturnsResults() throws Exception {
            var expectedDictionary = new LinkedHashMap<String, String>();

            expectedDictionary.put("English Phrase 1", "");
            expectedDictionary.put("English Phrase 2", "Translated Phrase 2");
            expectedDictionary.put("English Phrase 3", "Translated Phrase 1");

            mockMvc.perform(get(DICTIONARY_URL)
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
                () -> mockMvc.perform(get(DICTIONARY_URL)
                                          .contentType(MediaType.APPLICATION_JSON_VALUE))
            );

            assertTrue(nestedServletException.getCause() instanceof IllegalStateException);
            assertTrue(nestedServletException.getCause().getMessage().contains("Duplicate key English Phrase 1"));
        }

        @Test
        void shouldReturn403WhenUserDoesNotHaveManageTranslationsRole() throws Exception {
            stubUserInfo("unknown-role");
            mockMvc.perform(get(DICTIONARY_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(403))
                .andReturn();
        }
    }

    @Nested
    class GetTranslations {
        private static final String TRANSLATIONS_URL = "/translation/cy";

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = {"{}", "{\"phrases\":[]}", "{\"phrases\":[\"\"]}", "{\"illegal\":[\"English Phrase\"]}"})
        void shouldReturn400BadRequestWhenBadTranslationRequestIsSubmitted(final String input) throws Exception {
            mockMvc.perform(post(TRANSLATIONS_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE)
                                .content(input))
                .andExpect(status().isBadRequest());
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn200WhenLegalRequestIsSubmitted() throws Exception {
            final Map<String, String> expectedTranslations = Map.of("English Phrase 2", "Translated Phrase 2");

            mockMvc.perform(post(TRANSLATIONS_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE)
                                .content("{\"phrases\": [\"English Phrase 2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations", equalTo(expectedTranslations)));
        }
    }

    @Nested
    @DisplayName("Put Dictionary")
    class PutDictionary {
        private final String serviceJwtDefinition = generateDummyS2SToken("ccd_definition");

        private final String serviceJwtXuiWeb = generateDummyS2SToken("xui_webapp");

        // manage-translations
        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamMUserWithManageTranslationCreateANewRecord() throws Exception {


            stubUserInfo("manage-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header("ServiceAuthorization", serviceJwtXuiWeb)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 3))))
                .andExpect(status().is(201))
                .andReturn();

            val versionResult = assertDictionaryEntityWithTranslationPhrases("english_1");
            val versionResult1 = assertDictionaryEntityWithTranslationPhrases("english_2");
            assertEquals(versionResult, versionResult1);
        }


        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT,PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithManageTranslationUpdateARecord() throws Exception {
            stubUserInfo("manage-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header("ServiceAuthorization", serviceJwtXuiWeb)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 3))))
                .andExpect(status().is(201))
                .andReturn();

            val versionResult = assertDictionaryEntityWithTranslationPhrases("english_1");
            val versionResult1 = assertDictionaryEntityWithTranslationPhrases("english_2");
            assertEquals(versionResult, versionResult1);
        }


        // load-translations user
        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithLoadTranslationNewPhrases() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header("ServiceAuthorization", serviceJwtXuiWeb)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(
                                    getDictionaryRequestsWithoutTranslationPhrases(1, 3)))
            )
                .andExpect(status().is(201))
                .andReturn();

            assertDictionaryEntityWithOutTranslationPhrases("english_1");
            assertDictionaryEntityWithOutTranslationPhrases("english_2");
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT,PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithLoadTranslationExistingPhrases() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header("ServiceAuthorization", serviceJwtXuiWeb)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(
                                        getDictionaryRequestsWithoutTranslationPhrases(1, 2)))
            )
                .andExpect(status().is(201))
                .andReturn();
            // No action taken for existing phrases, verify previous translations are preserved.
            assertDictionaryEntityWithTranslationPhrases("english_1");
        }

        // 400 errors
        @Test
        @DisplayName("Incorrect payload: translations not permitted without `manage-translations` role")
        void shouldReturn400ForPutDictionaryForIdamUserWithLoadTranslationWithIncorrectPayLoad() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header("ServiceAuthorization", serviceJwtXuiWeb)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 2))))
                .andExpect(status().is(400))
                .andReturn();
        }

        @Test
        @DisplayName("Incorrect payload: translations not permitted for definition store client")
        void shouldReturn400ForPutDictionaryForIdamDefinitionStoreWithIncorrectPayLoad() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header("ServiceAuthorization", serviceJwtDefinition)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 2))))
                .andExpect(status().is(400))
                .andReturn();
        }


        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn400ForPutDictionaryForNonIdam() throws Exception {
            mockMvc.perform(put(DICTIONARY_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 2))))
                .andExpect(status().is(400))
                .andReturn();
        }

    }

    private Dictionary getDictionaryRequestsWithTranslationPhrases(int from, int to) {
        final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
        IntStream.range(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, "translated_" + i));
        return new Dictionary(expectedMapKeysAndValues);
    }

    private Dictionary getDictionaryRequestsWithoutTranslationPhrases(int from, int to) {
        final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
        IntStream.range(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, null));
        return new Dictionary(expectedMapKeysAndValues);
    }

    private Integer assertDictionaryEntityWithTranslationPhrases(String englishPhrase) {
        val dictionaryEntity = dictionaryRepository.findByEnglishPhrase(englishPhrase);
        assertTrue(dictionaryEntity.isPresent());
        assertNotNull(dictionaryEntity.get().getTranslationUpload());
        val version = dictionaryEntity.get().getTranslationUpload().getVersion();
        assertNotNull(version);
        assertNotNull(dictionaryEntity.get().getTranslationPhrase());
        return version;
    }


    private void assertDictionaryEntityWithOutTranslationPhrases(String englishPhrase) {
        val dictionaryEntity = dictionaryRepository.findByEnglishPhrase(englishPhrase);
        assertTrue(dictionaryEntity.isPresent());
        assertNull(dictionaryEntity.get().getTranslationUpload());
        assertNull(dictionaryEntity.get().getTranslationPhrase());
    }
}
