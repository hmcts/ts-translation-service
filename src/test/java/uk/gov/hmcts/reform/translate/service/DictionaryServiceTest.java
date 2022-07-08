package uk.gov.hmcts.reform.translate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.ApplicationParams;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;
import uk.gov.hmcts.reform.translate.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.translate.errorhandling.RequestErrorException;
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.repository.TranslationUploadRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.BAD_SCHEMA;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.WELSH_NOT_ALLOWED;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.LOAD_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.service.DictionaryService.TEST_PHRASES_START_WITH;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    private static final String CLIENTS2S_TOKEN = "clientS2SToken";
    private static final String DEFINITION_STORE = "definition_store";
    private static final String XUI = "xui";

    @Mock
    DictionaryRepository dictionaryRepository;

    @Mock
    TranslationUploadRepository translationUploadRepository;

    @Mock
    Iterable<DictionaryEntity> repositoryResults;

    @Mock
    DictionaryMapper dictionaryMapper;

    @Mock
    SecurityUtils securityUtils;

    @Mock
    ApplicationParams applicationParams;

    @InjectMocks
    DictionaryService dictionaryService;

    private static final String THE_QUICK_FOX_PHRASE = "the quick fox";

    @Nested
    @DisplayName("getDictionary")
    class GetDictionary {

        @BeforeEach
        void setUp() {
            given(securityUtils.hasRole(any())).willReturn(true);
        }

        @Test
        void shouldReturnDictionaryContents() {
            Map<String, String> expectedMapKeysAndValues = new HashMap<>();

            IntStream.range(1, 3).forEach(i -> expectedMapKeysAndValues.put("english" + i, "translated" + i));

            var dictionaryEntities = expectedMapKeysAndValues.entrySet().stream()
                .map(es -> createDictionaryEntity(
                    es.getKey(),
                    es.getValue()
                ))
                .toArray(DictionaryEntity[]::new);

            var spliterator = Arrays.spliterator(dictionaryEntities);
            given(repositoryResults.spliterator()).willReturn(spliterator);
            given(dictionaryRepository.findAll()).willReturn(repositoryResults);

            assertTrue(dictionaryService.getDictionaryContents().entrySet()
                           .containsAll(expectedMapKeysAndValues.entrySet()));
        }

        @Test
        void shouldReturnDictionaryContentsTranslationPhraseIsNull() {
            Map<String, String> expectedMapKeysAndValues = new HashMap<>();

            IntStream.range(1, 3).forEach(i -> expectedMapKeysAndValues.put("english" + i, null));

            var dictionaryEntities = expectedMapKeysAndValues.entrySet().stream()
                .map(es -> createDictionaryEntity(
                    es.getKey(),
                    es.getValue()
                ))
                .toArray(DictionaryEntity[]::new);

            var spliterator = Arrays.spliterator(dictionaryEntities);
            given(repositoryResults.spliterator()).willReturn(spliterator);
            given(dictionaryRepository.findAll()).willReturn(repositoryResults);

            Map<String, String> dictionaryContents = dictionaryService.getDictionaryContents();
            assertTrue(dictionaryContents.keySet()
                           .containsAll(expectedMapKeysAndValues.keySet()));
            assertTrue(dictionaryContents.values()
                           .containsAll(List.of("", "", "")));
        }

        @Test
        void shouldThrowExceptionWhenDictionaryContainsDuplicateEnglishPhrases() {
            final var englishPhrase = "English phrase";
            final var translatedPhrase = "Translated phrase";

            DictionaryEntity[] dictionaryEntities = {createDictionaryEntity(englishPhrase, translatedPhrase),
                createDictionaryEntity(englishPhrase, translatedPhrase),
                createDictionaryEntity(englishPhrase, translatedPhrase)};

            var spliterator = Arrays.spliterator(dictionaryEntities);
            given(repositoryResults.spliterator()).willReturn(spliterator);
            given(dictionaryRepository.findAll()).willReturn(repositoryResults);

            IllegalStateException illegalStateException = assertThrows(
                IllegalStateException.class,
                () -> dictionaryService.getDictionaryContents()
            );

            assertEquals(
                String.format("Duplicate key %s (attempted merging values %s and %s)",
                              englishPhrase, translatedPhrase, translatedPhrase
                ),
                illegalStateException.getMessage()
            );
        }

        @Test
        void shouldReturnEmptyDictionaryContents() {
            given(dictionaryRepository.findAll()).willReturn(repositoryResults);
            assertTrue(dictionaryService.getDictionaryContents().isEmpty());
        }

        @Test
        void shouldThrowExceptionWhenReturningDictionaryContentsNoUserInfoAvailable() {
            Mockito.reset(securityUtils);
            assertThrows(RoleMissingException.class, () -> dictionaryService.getDictionaryContents());
        }

        @Test
        void shouldThrowExceptionWhenReturningDictionaryContentsUsingIncorrectRole() {
            given(securityUtils.hasRole(any())).willReturn(false);
            RoleMissingException roleMissingException = assertThrows(
                RoleMissingException.class,
                () -> dictionaryService.getDictionaryContents()
            );
            assertEquals(
                String.format(RoleMissingException.ERROR_MESSAGE, MANAGE_TRANSLATIONS_ROLE),
                roleMissingException.getMessage()
            );
        }

    }

    @Nested
    @DisplayName("GetTranslation")
    class GetTranslations {

        @Test
        void testShouldTranslatePhrase() {
            final DictionaryEntity dictionaryEntity = createDictionaryEntity(THE_QUICK_FOX_PHRASE, "translated");
            doReturn(Optional.of(dictionaryEntity)).when(dictionaryRepository).findByEnglishPhrase(anyString());

            final String translation = dictionaryService.getTranslation(THE_QUICK_FOX_PHRASE);

            assertThat(translation)
                .isNotNull()
                .isEqualTo("translated");

            verify(dictionaryRepository).findByEnglishPhrase(THE_QUICK_FOX_PHRASE);
            verifyNoMoreInteractions(dictionaryRepository);
        }

        @Test
        void testShouldTranslatePhraseWhenTranslatedPhraseIsNull() {
            final DictionaryEntity dictionaryEntity = createDictionaryEntity(THE_QUICK_FOX_PHRASE, null);
            doReturn(Optional.of(dictionaryEntity)).when(dictionaryRepository).findByEnglishPhrase(anyString());

            final String translation = dictionaryService.getTranslation(THE_QUICK_FOX_PHRASE);

            assertThat(translation)
                .isNotNull()
                .isEqualTo(THE_QUICK_FOX_PHRASE);

            verify(dictionaryRepository).findByEnglishPhrase(THE_QUICK_FOX_PHRASE);
            verifyNoMoreInteractions(dictionaryRepository);
        }

        @Test
        void testShouldTranslatePhraseWhenEnglishPhraseIsNotInDictionary() {
            final DictionaryEntity dictionaryEntity = createDictionaryEntity(THE_QUICK_FOX_PHRASE, null);
            doReturn(Optional.empty()).when(dictionaryRepository).findByEnglishPhrase(anyString());
            doReturn(dictionaryEntity).when(dictionaryRepository).save(dictionaryEntity);

            final String translation = dictionaryService.getTranslation(THE_QUICK_FOX_PHRASE);

            assertThat(translation)
                .isNotNull()
                .isEqualTo(THE_QUICK_FOX_PHRASE);

            verify(dictionaryRepository).save(any());
            verify(dictionaryRepository).findByEnglishPhrase(THE_QUICK_FOX_PHRASE);
        }

        @Test
        void testShouldReturnTranslations() {
            // GIVEN
            final String englishPhrase = "English phrase";
            final String englishPhraseWithNoTranslation = "English phrase with no translation";
            final String englishPhraseNotInDictionary = "English phrase not in dictionary";

            final Map<String, String> expectedTranslations =
                Map.of(englishPhrase, "Translated English phrase",
                       englishPhraseWithNoTranslation, englishPhraseWithNoTranslation,
                       englishPhraseNotInDictionary, englishPhraseNotInDictionary
                );

            final DictionaryEntity entity1 = createDictionaryEntity(englishPhrase, "Translated English phrase");
            final DictionaryEntity entity2 = createDictionaryEntity(englishPhraseWithNoTranslation, null);
            final DictionaryEntity entity3 = createDictionaryEntity(englishPhraseNotInDictionary, null);

            doReturn(Optional.of(entity1)).when(dictionaryRepository).findByEnglishPhrase(englishPhrase);
            doReturn(Optional.of(entity2)).when(dictionaryRepository)
                .findByEnglishPhrase(englishPhraseWithNoTranslation);
            doReturn(Optional.empty()).when(dictionaryRepository)
                .findByEnglishPhrase(englishPhraseNotInDictionary);
            doReturn(entity3).when(dictionaryRepository).save(entity3);

            final Set<String> translationRequestPhrases = Set.of(
                englishPhrase,
                englishPhraseWithNoTranslation,
                englishPhraseNotInDictionary
            );

            // WHEN
            final Map<String, String> actualTranslations = dictionaryService.getTranslations(translationRequestPhrases);

            // THEN
            assertThat(actualTranslations)
                .isNotEmpty()
                .containsAllEntriesOf(expectedTranslations);

            verify(dictionaryRepository).findByEnglishPhrase(englishPhrase);
            verify(dictionaryRepository).findByEnglishPhrase(englishPhraseWithNoTranslation);
            verify(dictionaryRepository).findByEnglishPhrase(englishPhraseNotInDictionary);
            verify(dictionaryRepository).save(entity3);
        }

        @Test
        @SuppressWarnings("ConstantConditions")
        void testShouldRaiseExceptionWhenInputPhraseIsNull() {
            final Throwable thrown = catchThrowable(() -> dictionaryService.getTranslation(null));

            assertThat(thrown)
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @SuppressWarnings("ConstantConditions")
        void testShouldRaiseExceptionWhenInputPhrasesIsNull() {
            final Throwable thrown = catchThrowable(() -> dictionaryService.getTranslations(null));

            assertThat(thrown)
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("PutDictionary")
    class PutDictionary {

        @Test
        void shouldPutANewDictionaryForUserWithManageTranslationsRole() {

            // GIVEN
            final Dictionary dictionaryRequest = getDictionaryRequestWithTranslationPhrases(3);
            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(securityUtils.hasRole(anyString())).willReturn(true);
            given(dictionaryMapper.createTranslationUploadEntity(anyString())).willReturn(createUploadEntity());

            // WHEN
            dictionaryService.putDictionary(dictionaryRequest);

            // THEN
            verify(dictionaryRepository, times(3)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryMapper, times(3)).modelToEntityWithTranslationUploadEntity(any(), any());
            verify(dictionaryRepository, times(3)).save(any());
            verify(translationUploadRepository, never()).save(any());
        }

        @Test
        void shouldPutANewDictionaryForUserWithoutManageTranslationsRole() {

            // GIVEN
            final Dictionary dictionaryRequest = getDictionaryRequestWithoutTranslationPhrases(3);
            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(securityUtils.hasRole(anyString())).willReturn(false);

            // WHEN
            dictionaryService.putDictionary(dictionaryRequest);

            // THEN
            verify(dictionaryRepository, times(3)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryMapper, times(3)).modelToEntityWithoutTranslationPhrase(any());
            verify(dictionaryRepository, times(3)).save(any());
            // verify no translation uploaded entity created as no translations
            verify(dictionaryMapper, never()).createTranslationUploadEntity(anyString());
            verify(translationUploadRepository, never()).save(any());
        }

        @Test
        void shouldUpdateADictionaryForUserWithManageTranslationsRole() {

            // GIVEN
            final Dictionary dictionaryRequest = getDictionaryRequestWithTranslationPhrases(1);
            final DictionaryEntity dictionaryEntity =
                createDictionaryEntity("english_1", "translated_1");

            given(dictionaryRepository.findByEnglishPhrase(any())).willReturn(Optional.of(dictionaryEntity));
            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(securityUtils.hasRole(anyString())).willReturn(true);
            given(dictionaryMapper.createTranslationUploadEntity(anyString())).willReturn(createUploadEntity());

            // WHEN
            dictionaryService.putDictionary(dictionaryRequest);

            // THEN
            verify(dictionaryRepository, times(1)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryRepository, times(1)).save(any());
            verify(translationUploadRepository, times(1)).save(any());
        }

        @Test
        void shouldUpdateADictionaryForUserWithoutManageTranslationsRole() {

            // GIVEN
            final Dictionary dictionaryRequest = getDictionaryRequestWithoutTranslationPhrases(1);
            final DictionaryEntity dictionaryEntity =
                createDictionaryEntity("english_1", "translated_1");

            given(dictionaryRepository.findByEnglishPhrase(any())).willReturn(Optional.of(dictionaryEntity));
            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(securityUtils.hasRole(anyString())).willReturn(false);

            // WHEN
            dictionaryService.putDictionary(dictionaryRequest);

            // THEN
            verify(dictionaryRepository, times(1)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryRepository, times(0)).save(any());
            // verify no translation uploaded entity created as no translations
            verify(dictionaryMapper, never()).createTranslationUploadEntity(anyString());
            verify(translationUploadRepository, never()).save(any());
        }

        @Test
        void shouldPutDictionaryRoleCheckForAValidUserWithManageTranslationsRole() {

            // GIVEN
            given(applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck())
                .willReturn(List.of(DEFINITION_STORE));
            given(securityUtils.getServiceNameFromS2SToken(CLIENTS2S_TOKEN)).willReturn(XUI);
            given(securityUtils.hasAnyOfTheseRoles(anyList())).willReturn(true);

            // WHEN / THEN
            assertDoesNotThrow(
                () -> dictionaryService.putDictionaryRoleCheck(CLIENTS2S_TOKEN)
            );
        }

        @Test
        void shouldPutDictionaryRoleCheckForAValidUserWithLoadTranslationRole() {

            // GIVEN
            given(applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck())
                .willReturn(List.of(DEFINITION_STORE));
            given(securityUtils.hasAnyOfTheseRoles(anyList())).willReturn(true);
            given(securityUtils.getServiceNameFromS2SToken(CLIENTS2S_TOKEN)).willReturn(XUI);

            // WHEN / THEN
            assertDoesNotThrow(
                () -> dictionaryService.putDictionaryRoleCheck(CLIENTS2S_TOKEN)
            );
        }

        @Test
        void shouldPutDictionaryRoleCheckForAValidDefinitionStore() {

            // GIVEN
            given(applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck())
                .willReturn(List.of(DEFINITION_STORE));
            given(securityUtils.getServiceNameFromS2SToken(CLIENTS2S_TOKEN)).willReturn(DEFINITION_STORE);

            // WHEN / THEN
            assertDoesNotThrow(
                () -> dictionaryService.putDictionaryRoleCheck(CLIENTS2S_TOKEN)
            );
        }


        @Test
        void shouldFailPutDictionaryRoleCheckForAValidUserWithOutAnyRole() {

            // GIVEN
            given(applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck())
                .willReturn(List.of(DEFINITION_STORE));

            given(securityUtils.getServiceNameFromS2SToken(CLIENTS2S_TOKEN)).willReturn(XUI);
            given(securityUtils.hasAnyOfTheseRoles(anyList())).willReturn(false);

            // WHEN / THEN
            RequestErrorException roleMissingException = assertThrows(
                RequestErrorException.class, () -> dictionaryService.putDictionaryRoleCheck(CLIENTS2S_TOKEN)
            );

            // THEN
            assertThat(roleMissingException).isInstanceOf(RequestErrorException.class);
            assertEquals(
                String.format(
                    RequestErrorException.ERROR_MESSAGE, MANAGE_TRANSLATIONS_ROLE + "," + LOAD_TRANSLATIONS_ROLE
                ),
                roleMissingException.getMessage()
            );
        }

        @Test
        void shouldFailPutDictionaryDueToInvalidClientToken() {

            // GIVEN
            given(applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck())
                .willReturn(List.of(DEFINITION_STORE));

            given(securityUtils.getServiceNameFromS2SToken(CLIENTS2S_TOKEN)).willReturn(XUI);
            given(securityUtils.hasAnyOfTheseRoles(anyList())).willReturn(false);

            // WHEN / THEN
            RequestErrorException roleMissingException = assertThrows(
                RequestErrorException.class, () -> dictionaryService.putDictionaryRoleCheck(CLIENTS2S_TOKEN)
            );

            // THEN
            assertThat(roleMissingException).isInstanceOf(RequestErrorException.class);
            assertEquals(
                String.format(
                    RequestErrorException.ERROR_MESSAGE, MANAGE_TRANSLATIONS_ROLE + "," + LOAD_TRANSLATIONS_ROLE
                ),
                roleMissingException.getMessage()
            );
        }


        // Incorrect pay_load
        @Test
        void shouldFailUpdateADictionaryForUserWithoutManageTranslationsRoleAndNullPayload() {

            // GIVEN
            final Dictionary dictionaryRequest = new Dictionary(null);
            given(applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck())
                .willReturn(List.of(DEFINITION_STORE));
            given(securityUtils.getServiceNameFromS2SToken(CLIENTS2S_TOKEN)).willReturn(DEFINITION_STORE);
            given(securityUtils.hasRole(anyString())).willReturn(false);
            dictionaryService.putDictionaryRoleCheck(CLIENTS2S_TOKEN);

            // WHEN / THEN
            BadRequestException badRequestException = assertThrows(
                BadRequestException.class, () -> dictionaryService.putDictionary(dictionaryRequest)
            );

            // THEN
            assertThat(badRequestException).isInstanceOf(BadRequestException.class);
            assertEquals(BAD_SCHEMA, badRequestException.getMessage());
        }

        @Test
        void shouldFailUpdateADictionaryForUserWithoutManageTranslationsRoleAndIncorrectPayload() {


            final Dictionary dictionaryRequest = new Dictionary(new HashMap<>());
            given(applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck())
                .willReturn(List.of(DEFINITION_STORE));
            given(securityUtils.getServiceNameFromS2SToken(CLIENTS2S_TOKEN)).willReturn(DEFINITION_STORE);
            given(securityUtils.hasRole(anyString())).willReturn(false);
            dictionaryService.putDictionaryRoleCheck(CLIENTS2S_TOKEN);

            BadRequestException badRequestException = assertThrows(
                BadRequestException.class, () -> dictionaryService.putDictionary(dictionaryRequest)
            );

            assertThat(badRequestException).isInstanceOf(BadRequestException.class);
            assertEquals(BAD_SCHEMA, badRequestException.getMessage());
        }

        @Test
        void shouldFailUpdateADictionaryForDefinitionStoreWithIncorrectPayload() {

            // GIVEN
            final Dictionary dictionaryRequest = getDictionaryRequestWithTranslationPhrases(1);
            given(securityUtils.hasRole(anyString())).willReturn(false);

            // WHEN / THEN
            BadRequestException badRequestException = assertThrows(
                BadRequestException.class, () -> dictionaryService.putDictionary(dictionaryRequest)
            );

            // THEN
            assertThat(badRequestException).isInstanceOf(BadRequestException.class);
            assertEquals(WELSH_NOT_ALLOWED, badRequestException.getMessage());
        }

        private Dictionary getDictionaryRequestWithTranslationPhrases(int count) {
            final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
            IntStream.range(1, count + 1).forEach(i -> expectedMapKeysAndValues.put("english_" + i, "translated_" + i));
            return new Dictionary(expectedMapKeysAndValues);
        }

        private Dictionary getDictionaryRequestWithoutTranslationPhrases(int count) {
            final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
            IntStream.range(1, count + 1).forEach(i -> expectedMapKeysAndValues.put("english_" + i, null));
            return new Dictionary(expectedMapKeysAndValues);
        }

        private UserInfo getUserInfoWithManageTranslationsRole() {
            return UserInfo.builder()
                .familyName("NE_NU_NE")
                .name("PEPE")
                .givenName("givenName")
                .uid("11111111")
                .roles(Arrays.asList("ROLE", "manage-translations"))
                .sub("sub")
                .build();
        }
    }

    @Nested
    @DisplayName("DeleteTestPhrases")
    class DeleteTestPhrases {

        @Test
        void shouldDeleteTestPhrasesWhenUsingCorrectRole() {

            // GIVEN
            given(securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE)).willReturn(true);

            // WHEN
            dictionaryService.deleteTestPhrases();

            // THEN
            verify(dictionaryRepository).deleteByEnglishPhraseStartingWith(TEST_PHRASES_START_WITH);

        }

        @Test
        void shouldThrowExceptionWhenUsingIncorrectRole() {

            // GIVEN
            given(securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE)).willReturn(false);

            // WHEN
            RoleMissingException roleMissingException = assertThrows(
                RoleMissingException.class,
                () -> dictionaryService.deleteTestPhrases()
            );

            // THEN
            assertEquals(
                String.format(RoleMissingException.ERROR_MESSAGE, MANAGE_TRANSLATIONS_ROLE),
                roleMissingException.getMessage()
            );

        }

    }

    private DictionaryEntity createDictionaryEntity(String phrase, String translationPhrase) {
        final var dictionaryEntity = new DictionaryEntity();
        dictionaryEntity.setEnglishPhrase(phrase);
        dictionaryEntity.setTranslationPhrase(translationPhrase);
        return dictionaryEntity;
    }

    private TranslationUploadEntity createUploadEntity() {
        final var translationUploadEntity = new TranslationUploadEntity();
        translationUploadEntity.setVersion(123L);
        return translationUploadEntity;
    }

}

