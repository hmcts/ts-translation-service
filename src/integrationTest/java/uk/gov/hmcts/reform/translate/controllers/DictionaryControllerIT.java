package uk.gov.hmcts.reform.translate.controllers;

import com.pivovarit.function.ThrowingSupplier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.translate.BaseTest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.translate.model.ControllerConstants.DICTIONARY_URL;
import static uk.gov.hmcts.reform.translate.model.ControllerConstants.TRANSLATIONS_URL;

public class DictionaryControllerIT extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Get Dictionary")
    class GetDictionary {
        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn200WhenDictionaryReturnsNoResults() throws Exception {
            mockMvc.perform(get(DICTIONARY_URL)
                                .contentType(APPLICATION_JSON_VALUE))
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
                                .contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.translations", equalTo(expectedDictionary)))
                .andReturn();
        }

        @Test
        void shouldReturn403WhenUserDoesNotHaveManageTranslationsRole() throws Exception {
            stubUserInfo("unknown-role");
            mockMvc.perform(get(DICTIONARY_URL)
                                .contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().is(403))
                .andReturn();
        }
    }

    @Nested
    class GetTranslations {

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = {"{}", "{\"phrases\":[]}", "{\"phrases\":[\"\"]}", "{\"illegal\":[\"English Phrase\"]}"})
        void shouldReturn400BadRequestWhenBadTranslationRequestIsSubmitted(final String input) throws Exception {
            mockMvc.perform(post(TRANSLATIONS_URL)
                                .contentType(APPLICATION_JSON_VALUE)
                                .accept(APPLICATION_JSON_VALUE)
                                .content(input))
                .andExpect(status().isBadRequest());
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn200WhenLegalRequestIsSubmitted() throws Exception {
            final Map<String, String> expectedTranslations = Map.of("English Phrase 2", "Translated Phrase 2");

            mockMvc.perform(post(TRANSLATIONS_URL)
                                .contentType(APPLICATION_JSON_VALUE)
                                .accept(APPLICATION_JSON_VALUE)
                                .content("{\"phrases\": [\"English Phrase 2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations", equalTo(expectedTranslations)));
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldTestConcurrentAddToDictionaryViaTranslateEndpoint() {
            final String payload = "{\"phrases\": [\"English phrase 2\"]}";

            final CompletableFuture<ResultActions> future1 = CompletableFuture.supplyAsync(
                ThrowingSupplier.unchecked(() -> mockMvc.perform(post(TRANSLATIONS_URL)
                                                                     .contentType(APPLICATION_JSON_VALUE)
                                                                     .accept(APPLICATION_JSON_VALUE)
                                                                     .content(payload)))
            );
            final CompletableFuture<ResultActions> future2 = CompletableFuture.supplyAsync(
                ThrowingSupplier.unchecked(() -> mockMvc.perform(post(TRANSLATIONS_URL)
                                                                     .contentType(APPLICATION_JSON_VALUE)
                                                                     .accept(APPLICATION_JSON_VALUE)
                                                                     .content(payload)))
            );

            final Throwable thrown = catchThrowable(() -> CompletableFuture.allOf(future1, future2).get());

            // THEN
            assertThatDataIntegrityViolationExceptionWasThrown(thrown);
        }
    }


    @Nested
    @Disabled // Pending WLTS-25
    class PutDictionary {

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldTestConcurrentAddToDictionaryViaPutEndpoint() {
            final String payload = "{\"English phrase 2\": \"Translated Phrase 2\","
                + " \"English Phrase 3\": \"Translated Phrase 3\"}";

            final CompletableFuture<ResultActions> future1 = CompletableFuture.supplyAsync(
                ThrowingSupplier.unchecked(() -> mockMvc.perform(put(DICTIONARY_URL)
                                                                     .contentType(APPLICATION_JSON_VALUE)
                                                                     .accept(APPLICATION_JSON_VALUE)
                                                                     .content(payload)))
            );
            final CompletableFuture<ResultActions> future2 = CompletableFuture.supplyAsync(
                ThrowingSupplier.unchecked(() -> mockMvc.perform(put(DICTIONARY_URL)
                                                                     .contentType(APPLICATION_JSON_VALUE)
                                                                     .accept(APPLICATION_JSON_VALUE)
                                                                     .content(payload)))
            );

            final Throwable thrown = catchThrowable(() -> CompletableFuture.allOf(future1, future2).get());

            // THEN
            assertThatDataIntegrityViolationExceptionWasThrown(thrown);
        }
    }

    private void assertThatDataIntegrityViolationExceptionWasThrown(final Throwable thrown) {
        assertThat(thrown)
            .isNotNull()
            .isInstanceOfSatisfying(
                ExecutionException.class,
                exception -> assertThat(exception.getCause().getCause().getCause())
                    .isInstanceOf(DataIntegrityViolationException.class)
            );
    }
}
