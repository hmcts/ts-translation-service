package uk.gov.hmcts.reform.translate.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.translate.BaseTest;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.model.Translation;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.repository.JpaDictionaryRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    @Qualifier(JpaDictionaryRepository.QUALIFIER)
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

            mockMvc.perform(get(DICTIONARY_URL)
                                .contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.translations['English Phrase 2'].translation",
                                    equalTo(GET_DICTIONARY_TEST_PHRASE_2_TRANSLATION)))
                .andExpect(jsonPath("$.translations['English Phrase 3'].translation",
                                    equalTo(GET_DICTIONARY_TEST_PHRASE_3_TRANSLATION)))
                .andExpect(jsonPath("$.translations['English Phrase 3'].yes",
                                    equalTo("Yes Translation")))
                .andExpect(jsonPath("$.translations['English Phrase 3'].no",
                                    equalTo("No Translation")))
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

            mockMvc.perform(post(TRANSLATIONS_URL)
                                .contentType(APPLICATION_JSON_VALUE)
                                .accept(APPLICATION_JSON_VALUE)
                                .content("{\"phrases\": [\"English Phrase 3\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations['English Phrase 3'].translation",
                                    equalTo(GET_DICTIONARY_TEST_PHRASE_3_TRANSLATION)))
                .andExpect(jsonPath("$.translations['English Phrase 3'].yes",
                                    equalTo("Yes Translation")))
                .andExpect(jsonPath("$.translations['English Phrase 3'].no",
                                    equalTo("No Translation")));
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
                                .content(objectMapper.writeValueAsString(getDictionaryRequests(
                                    2,
                                    new Translation("translation")
                                ))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            assertDictionaryEntity(
                "english_1",
                new Translation("translation_1"),
                1
            );
            assertDictionaryEntity(
                "english_2",
                new Translation("translation_2"),
                1
            );
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn201ForPutDictionaryForCreatingNewYesOrNoRecords() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        2,
                                        new Translation("translation", true, "yes", "no")
                                    ))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            assertDictionaryEntity(
                "english_1",
                new Translation("translation_1", true, "yes_1", "no_1"),
                1
            );
            assertDictionaryEntity(
                "english_2",
                new Translation("translation_2", true, "yes_2", "no_2"),
                1
            );
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
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        2,
                                            new Translation("translation")
                                        ))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            assertDictionaryEntity(
                "english_1",
                new Translation("translation_1"),
                2
            );
            assertDictionaryEntity(
                "english_2",
                new Translation("translation_2"),
                2
            );
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryWhenUpdatingExistingRecordsWithYesOrNo() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        2,
                                        new Translation("translation", true, "yes", "no")
                                    ))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            assertDictionaryEntity(
                "english_1",
                new Translation("translation_1", true, "yes_1", "no_1"),
                2
            );
            assertDictionaryEntity(
                "english_2",
                new Translation("translation_2", true, "yes_2", "no_2"),
                2
            );
        }

        @Test
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, PUT_CREATE_ENGLISH_PHRASES_WITH_TRANSLATIONS_SCRIPT})
        void shouldReturn201ForPutDictionaryForUpdatingExistingRecordsWithOnlyYesOrNo() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        4,
                                        new Translation("", true, null, null)
                                    ))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN // Mark existing translation as Yes Or No
            assertDictionaryEntity(
                "english_1",
                new Translation("translated_1", true, null, null),
                1
            );
            assertDictionaryEntity(
                "english_2",
                new Translation("translated_2", true, null, null),
                1
            );
            // THEN // Create new Yes Or No Entries
            assertDictionaryEntity(
                "english_3",
                new Translation("", true, null, null),
                null
            );
            assertDictionaryEntity(
                "english_4",
                new Translation("", true, null, null),
                null
            );
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
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        4,
                                        new Translation("translation")
                                        ))))
                .andExpect(status().is(201))
                .andReturn();

            // THEN // UPDATED (in SQL file)
            assertDictionaryEntity(
                "english_1",
                new Translation("translation_1"),
                2
            );
            assertDictionaryEntity(
                "english_2",
                new Translation("translation_2"),
                2
            );
            // THEN // NEW
            assertDictionaryEntity(
                "english_3",
                new Translation("translation_3"),
                2
            );
            assertDictionaryEntity(
                "english_4",
                new Translation("translation_4"),
                2
            );
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
                                    getDictionaryRequests(
                                        3,
                                        new Translation("")
                                    )))
                )
                .andExpect(status().is(201))
                .andReturn();

            // THEN
            assertDictionaryEntity("english_1", new Translation(""), null);
            assertDictionaryEntity("english_2", new Translation(""), null);
            assertDictionaryEntity("english_3", new Translation(""), null);
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
                                        getDictionaryRequests(2, new Translation(""))))
                )
                .andExpect(status().is(201))
                .andReturn();

            // THEN // No action taken for existing phrases, verify previous translations are preserved.
            assertDictionaryEntity("english_1", new Translation("translated_1"), 1);
            assertDictionaryEntity("english_2", new Translation("translated_2"), 1);
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
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        1,
                                        new Translation("expected to fail")
                                    ))))
                .andExpect(status().is(400))
                .andReturn();
        }

        @Test
        @DisplayName("Incorrect payload: yes no not permitted without `manage-translations` role")
        void shouldReturn400ForPutDictionaryForIdamUserWithLoadTranslationWithIncorrectYesNoPayLoad() throws Exception {

            // GIVEN
            stubUserInfo(LOAD_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(put(DICTIONARY_URL)
                                .header(SERVICE_AUTHORIZATION, serviceJwtXuiWeb)
                                .contentType(APPLICATION_JSON_VALUE)
                                .content(
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        1,
                                        new Translation("", true, "bad yes", "bad no")
                                    ))))
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
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        1,
                                        new Translation("expected to fail")
                                    ))))
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
                                    objectMapper.writeValueAsString(getDictionaryRequests(
                                        1,
                                        new Translation("expected to fail")
                                    ))))
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

    private Dictionary getDictionaryRequests(int count, Translation toMatch) {
        final Map<String, Translation> expectedMap = new HashMap<>();
        IntStream.range(1, count + 1).forEach(i -> {
            String text = StringUtils.isBlank(toMatch.getTranslation()) ? "" : toMatch.getTranslation() + "_" + i;
            String yesText = StringUtils.isBlank(toMatch.getYes()) ? "" : toMatch.getYes() + "_" + i;
            String noText = StringUtils.isBlank(toMatch.getNo()) ? "" : toMatch.getNo() + "_" + i;
            Translation translation = new Translation(text, toMatch.getYesOrNo(), yesText, noText);
            expectedMap.put("english_" + i, translation);
        });
        return new Dictionary(expectedMap);
    }

    private Long assertDictionaryEntity(String englishPhrase, Translation toMatch, Integer versionMatch) {
        final var dictionaryEntityOptional = dictionaryRepository.findByEnglishPhrase(englishPhrase);
        assertTrue(dictionaryEntityOptional.isPresent());
        final var dictionaryEntity = dictionaryEntityOptional.get();
        assertEquals(
            toMatch.getTranslation(),
            StringUtils.getIfEmpty(dictionaryEntity.getTranslationPhrase(), () -> "")
        );
        assertEquals(toMatch.isYesOrNo(), dictionaryEntity.isYesOrNo());
        assertEquals(toMatch.getYes(), dictionaryEntity.getYes());
        assertEquals(toMatch.getNo(), dictionaryEntity.getNo());

        if (versionMatch != null) {
            final var version = dictionaryEntity.getTranslationUpload().getVersion();
            assertNotNull(version);
            assertEquals(versionMatch.intValue(), version.intValue());
            return version;
        } else {
            assertNull(dictionaryEntity.getTranslationUpload());
            return null;
        }
    }

}
