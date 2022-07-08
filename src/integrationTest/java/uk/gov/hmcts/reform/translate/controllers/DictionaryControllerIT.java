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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static uk.gov.hmcts.reform.translate.controllers.ControllerConstants.DICTIONARY_URL;
import static uk.gov.hmcts.reform.translate.controllers.ControllerConstants.TRANSLATIONS_URL;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.LOAD_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.SERVICE_AUTHORIZATION;

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
        private final String serviceJwtDefinition = generateDummyS2SToken("ccd_definition");

        private final String serviceJwtXuiWeb = generateDummyS2SToken("xui_webapp");

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldTestConcurrentAddToDictionaryViaPutEndpoint() throws Exception {
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);
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
        void shouldReturn201ForPutDictionaryForIdamUserWithManageTranslationCreateNewRecords() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(2))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            val versionResult1 = assertDictionaryEntityWithTranslationPhrases("english_1");
            val versionResult2 = assertDictionaryEntityWithTranslationPhrases("english_2");
            // NB: version numbers should be equal across single upload
            assertEquals(versionResult1, versionResult2);
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithManageTranslationUpdateExistingRecords() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(2))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            val versionResult1 = assertDictionaryEntityWithTranslationPhrases("english_1");
            val versionResult2 = assertDictionaryEntityWithTranslationPhrases("english_2");
            // NB: version numbers should be equal across single upload
            assertEquals(versionResult1, versionResult2);
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithManageTranslationNewAndUpdatedRecords() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(4))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            val versionResult1 = assertDictionaryEntityWithTranslationPhrases("english_1"); // UPDATED (in SQL file)
            val versionResult2 = assertDictionaryEntityWithTranslationPhrases("english_2"); // UPDATED (in SQL file)
            val versionResult3 = assertDictionaryEntityWithTranslationPhrases("english_3"); // NEW
            val versionResult4 = assertDictionaryEntityWithTranslationPhrases("english_4"); // NEW
            // NB: version numbers should be equal across single upload
            assertEquals(versionResult1, versionResult2);
            assertEquals(versionResult2, versionResult3);
            assertEquals(versionResult3, versionResult4);
        }

        // load-translations user
        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithLoadTranslationNewPhrases() throws Exception {

            // GIVEN
            stubUserInfo(LOAD_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(
                                    getDictionaryRequestsWithoutTranslationPhrases(3)))
                )
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            assertDictionaryEntityWithOutTranslationPhrases("english_1");
            assertDictionaryEntityWithOutTranslationPhrases("english_2");
            assertDictionaryEntityWithOutTranslationPhrases("english_3");
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForIdamUserWithLoadTranslationExistingPhrases() throws Exception {

            // GIVEN
            stubUserInfo(LOAD_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(
                                        getDictionaryRequestsWithoutTranslationPhrases(2)))
                )
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            // No action taken for existing phrases, verify previous translations are preserved.
            assertDictionaryEntityWithTranslationPhrases("english_1"); // NO CHANGE (in SQL file)
            assertDictionaryEntityWithTranslationPhrases("english_2"); // NO CHANGE (in SQL file)
        }

        // 400 errors
        @Test
        @DisplayName("Incorrect payload: translations not permitted without `manage-translations` role")
        void shouldReturn400ForPutDictionaryForIdamUserWithLoadTranslationWithIncorrectPayLoad() throws Exception {

            // GIVEN
            stubUserInfo(LOAD_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1))))
                .andExpect(status().is(400))
                .andReturn();
        }

        @Test
        @DisplayName("Incorrect payload: translations not permitted for definition store client")
        void shouldReturn400ForPutDictionaryForIdamDefinitionStoreWithIncorrectPayLoad() throws Exception {

            // GIVEN
            stubUserInfo(LOAD_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtDefinition)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1))))
                .andExpect(status().is(400))
                .andReturn();
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn400ForPutDictionaryForNonIdam() throws Exception {

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequestsWithTranslationPhrases(1))))
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

    private Dictionary getDictionaryRequestsWithTranslationPhrases(int count) {
        final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
        IntStream.range(1, count + 1).forEach(i -> expectedMapKeysAndValues.put("english_" + i, "translated_" + i));
        return new Dictionary(expectedMapKeysAndValues);
    }

    private Dictionary getDictionaryRequestsWithoutTranslationPhrases(int count) {
        final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
        IntStream.range(1, count + 1).forEach(i -> expectedMapKeysAndValues.put("english_" + i, null));
        return new Dictionary(expectedMapKeysAndValues);
    }

    private Long assertDictionaryEntityWithTranslationPhrases(String englishPhrase) {
        val dictionaryEntityOptional = dictionaryRepository.findByEnglishPhrase(englishPhrase);
        assertTrue(dictionaryEntityOptional.isPresent());
        val dictionaryEntity = dictionaryEntityOptional.get();
        assertNotNull(dictionaryEntity.getTranslationUpload());
        val version = dictionaryEntity.getTranslationUpload().getVersion();
        assertNotNull(version);
        assertNotNull(dictionaryEntity.getTranslationPhrase());
        return version;
    }


    private void assertDictionaryEntityWithOutTranslationPhrases(String englishPhrase) {
        val dictionaryEntity = dictionaryRepository.findByEnglishPhrase(englishPhrase);
        assertTrue(dictionaryEntity.isPresent());
        assertNull(dictionaryEntity.get().getTranslationUpload());
        assertNull(dictionaryEntity.get().getTranslationPhrase());
    }
}
