package uk.gov.hmcts.reform.translate.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import lombok.val;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.translate.BaseTest;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
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
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected DictionaryRepository dictionaryRepository;

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
    @DisplayName("Request Translations")
    class RequestTranslations {

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
        void shouldTestConcurrentAddToDictionaryViaTranslateEndpoint() throws Exception {
            final ExecutorService executorService = Executors.newFixedThreadPool(4);
            try {
                // GIVEN
                Collection<Callable<ResultActions>> callables = new ArrayList<>();
                IntStream.rangeClosed(1, 4)
                    .forEach(i -> callables.add(createRequestTranslationCallable()));

                // WHEN
                final List<Future<ResultActions>> taskFutureList = executorService.invokeAll(callables);

                // THEN
                assertThatResultsContainsConstraintViolation(taskFutureList);
            } finally {
                Objects.requireNonNull(executorService).shutdown();
            }
        }
    }

    private Callable<ResultActions> createRequestTranslationCallable() {
        return new Callable<>() {
            final String payload = "{\"phrases\": [\"English phrase 2\"]}";

            public ResultActions call() throws Exception {
                return mockMvc.perform(post(TRANSLATIONS_URL)
                                           .contentType(APPLICATION_JSON_VALUE)
                                           .accept(APPLICATION_JSON_VALUE)
                                           .content(payload));
            }
        };
    }

    @Nested
    @DisplayName("Put Dictionary")
    class PutDictionary {
        private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

        private final String serviceJwtDefinition = generateDummyS2SToken("ccd_definition");

        private final String serviceJwtXuiWeb = generateDummyS2SToken("xui_webapp");

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldTestConcurrentAddToDictionaryViaPutEndpoint() throws Exception {
            stubUserInfo("manage-translations");
            final ExecutorService executorService = Executors.newFixedThreadPool(4);
            try {
                // GIVEN
                Collection<Callable<ResultActions>> callables = new ArrayList<>();
                IntStream.rangeClosed(1, 4)
                    .forEach(i -> callables.add(createPutDictionaryCallable()));

                // WHEN
                final List<Future<ResultActions>> taskFutureList = executorService.invokeAll(callables);

                // THEN
                assertThatResultsContainsConstraintViolation(taskFutureList);
            } finally {
                Objects.requireNonNull(executorService).shutdown();
            }
        }

        private Callable<ResultActions> createPutDictionaryCallable() {
            return new Callable<>() {
                final String payload = "{\"translations\":{\"English phrase 2\": \"Translated Phrase 2\","
                    + " \"English Phrase 3\": \"Translated Phrase 3\"}}";

                public ResultActions call() throws Exception {
                    final Jwt jwt = dummyJwt();
                    when(authentication.getPrincipal()).thenReturn(jwt);
                    SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

                    return mockMvc.perform(put(DICTIONARY_URL)
                                               .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                               .contentType(APPLICATION_JSON_VALUE)
                                               .content(payload));
                }
            };
        }

        // manage-translations
        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamMUserWithManageTranslationCreateANewRecord() throws Exception {
            stubUserInfo("manage-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 3))))
                .andExpect(status().is(201))
                .andReturn();

            assertDictionaryEntityWithTranslationPhrases("english_1");
            assertDictionaryEntityWithTranslationPhrases("english_2");
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithManageTranslationUpdateARecord() throws Exception {
            stubUserInfo("manage-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 3))))
                .andExpect(status().is(201))
                .andReturn();

            assertDictionaryEntityWithTranslationPhrases("english_1");
            assertDictionaryEntityWithTranslationPhrases("english_2");
        }

        // load-translations user
        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithLoadTranslationNewPhrases() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(
                                    getDictionaryRequestsWithoutTranslationPhrases(1, 3)))
                )
                .andExpect(status().is(201))
                .andReturn();

            assertDictionaryEntityWithOutTranslationPhrases("english_1");
            assertDictionaryEntityWithOutTranslationPhrases("english_2");
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithLoadTranslationExistingPhrases() throws Exception {

            stubUserInfo("load-translations");
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
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
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
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
                                .header(SERVICE_AUTHORIZATION, serviceJwtDefinition)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 2))))
                .andExpect(status().is(400))
                .andReturn();
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn400ForPutDictionaryForNonIdam() throws Exception {
            mockMvc.perform(put(DICTIONARY_URL)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1, 2))))
                .andExpect(status().is(400))
                .andReturn();
        }

    }

    private void assertThatResultsContainsConstraintViolation(final List<Future<ResultActions>> taskFutureList) {
        final List<Either<Throwable, ResultActions>> results = collateResults(taskFutureList);
        assertThat(results)
            .isNotNull()
            .satisfies(items -> items.stream()
                .filter(Either::isLeft)
                .findFirst()
                .map(item -> VavrAssertions.assertThat(item)
                    .isNotNull()
                    .hasLeftValueSatisfying(DictionaryControllerIT.this::assertConstraintViolation)));
    }

    private List<Either<Throwable, ResultActions>> collateResults(final List<Future<ResultActions>> taskFutureList) {
        return taskFutureList.stream()
            .map(future -> {
                try {
                    final ResultActions resultActions = future.get(4, TimeUnit.SECONDS);
                    return Either.<Throwable, ResultActions>right(resultActions);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    return Either.<Throwable, ResultActions>left(e);
                }
            })
            .toList();
    }

    private void assertConstraintViolation(final Throwable thrown) {
        assertThat(thrown)
            .isNotNull()
            .isInstanceOfSatisfying(
                ExecutionException.class,
                exception -> assertThat(exception.getCause().getCause())
                    .isInstanceOf(DataIntegrityViolationException.class)
            );
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

    private void assertDictionaryEntityWithTranslationPhrases(String englishPhrase) {

        val dictionaryEntity = dictionaryRepository.findByEnglishPhrase(englishPhrase);
        assertTrue(dictionaryEntity.isPresent());
        assertNotNull(dictionaryEntity.get().getTranslationUpload());
        assertNotNull(dictionaryEntity.get().getTranslationUpload().getVersion());
        assertNotNull(dictionaryEntity.get().getTranslationPhrase());
    }


    private void assertDictionaryEntityWithOutTranslationPhrases(String englishPhrase) {
        val dictionaryEntity = dictionaryRepository.findByEnglishPhrase(englishPhrase);
        assertTrue(dictionaryEntity.isPresent());
        assertNull(dictionaryEntity.get().getTranslationUpload());
        assertNull(dictionaryEntity.get().getTranslationPhrase());
    }
}
