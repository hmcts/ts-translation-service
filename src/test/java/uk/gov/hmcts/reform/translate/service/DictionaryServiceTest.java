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
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;
import uk.gov.hmcts.reform.translate.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.translate.errorhandling.RequestErrorException;
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.model.Translation;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.repository.TranslationUploadRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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


@DisplayName("DictionaryService")
@ExtendWith(MockitoExtension.class)
public class DictionaryServiceTest {

    private static final String CLIENTS2S_TOKEN = "clientS2SToken";
    private static final String DEFINITION_STORE = "definition_store";
    private static final String XUI = "xui";

    @Mock
    DictionaryRepository dictionaryRepository;

    @Mock
    TranslationUploadRepository translationUploadRepository;

    @Mock
    List<DictionaryEntity> repositoryResults;

    @Mock
    DictionaryMapper dictionaryMapper;

    @Mock
    SecurityUtils securityUtils;

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
            Map<String, Translation> expectedMap = new HashMap<>();

            IntStream.range(1, 3).forEach(i -> expectedMap.put("english" + i, new Translation("translated" + i)));

            var dictionaryEntities = expectedMap.entrySet().stream()
                .map(es -> createDictionaryEntity(
                    es.getKey(),
                    es.getValue().getTranslation()
                ))
                .toArray(DictionaryEntity[]::new);

            var spliterator = Arrays.spliterator(dictionaryEntities);
            given(repositoryResults.spliterator()).willReturn(spliterator);
            given(dictionaryRepository.findAll()).willReturn(repositoryResults);

            assertTrue(dictionaryService.getDictionaryContents().entrySet()
                           .containsAll(expectedMap.entrySet()));
        }

        @Test
        void shouldReturnDictionaryContentsTranslationPhraseIsNull() {
            Map<String, Translation> expectedMap = new HashMap<>();

            IntStream.range(1, 3).forEach(i -> expectedMap.put("english" + i, new Translation("")));

            var dictionaryEntities = expectedMap.entrySet().stream()
                .map(es -> createDictionaryEntity(
                    es.getKey(),
                    es.getValue().getTranslation()
                ))
                .toArray(DictionaryEntity[]::new);

            var spliterator = Arrays.spliterator(dictionaryEntities);
            given(repositoryResults.spliterator()).willReturn(spliterator);
            given(dictionaryRepository.findAll()).willReturn(repositoryResults);

            Map<String, Translation> dictionaryContents = dictionaryService.getDictionaryContents();
            assertTrue(dictionaryContents.keySet()
                           .containsAll(expectedMap.keySet()));
            assertTrue(dictionaryContents.values().stream().map(Translation::getTranslation).toList()
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

            assertTrue(illegalStateException.getMessage().startsWith(
                String.format("Duplicate key %s (attempted merging values", englishPhrase)
            ));
        }

        @Test
        void shouldReturnEmptyDictionaryContents() {
            given(dictionaryRepository.findAll()).willReturn(repositoryResults);
            assertTrue(dictionaryService.getDictionaryContents().isEmpty());
        }

        @Test
        void shouldReturnDictionaryContentsWithLimit() {
            Map<String, Translation> expectedMap = new HashMap<>();

            IntStream.range(1, 3).forEach(i -> expectedMap.put("english" + i, new Translation("translated" + i)));

            var dictionaryEntities = expectedMap.entrySet().stream()
                .map(es -> createDictionaryEntity(
                    es.getKey(),
                    es.getValue().getTranslation()
                ))
                .toList();

            var pageable = PageRequest.of(0, 2, org.springframework.data.domain.Sort.by("englishPhrase"));
            given(dictionaryRepository.findAll(pageable))
                .willReturn(new PageImpl<>(dictionaryEntities, pageable, dictionaryEntities.size()));

            Map<String, Translation> dictionaryContents = dictionaryService.getDictionaryContents(2);
            assertTrue(dictionaryContents.entrySet().containsAll(expectedMap.entrySet()));
            verify(dictionaryRepository).findAll(pageable);
        }

        @Test
        void shouldUseFindAllWhenLimitIsNonPositive() {
            given(dictionaryRepository.findAll()).willReturn(repositoryResults);

            dictionaryService.getDictionaryContents(0);

            verify(dictionaryRepository).findAll();
            verify(dictionaryRepository, never()).findAll(any());
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

            final Translation translation = dictionaryService.getTranslation(THE_QUICK_FOX_PHRASE);

            assertThat(translation.getTranslation())
                .isNotNull()
                .isEqualTo("translated");

            verify(dictionaryRepository).findByEnglishPhrase(THE_QUICK_FOX_PHRASE);
            verifyNoMoreInteractions(dictionaryRepository);
        }

        @Test
        void testShouldTranslatePhraseWhenTranslatedPhraseIsNull() {
            final DictionaryEntity dictionaryEntity = createDictionaryEntity(THE_QUICK_FOX_PHRASE, null);
            doReturn(Optional.of(dictionaryEntity)).when(dictionaryRepository).findByEnglishPhrase(anyString());

            final Translation translation = dictionaryService.getTranslation(THE_QUICK_FOX_PHRASE);

            assertThat(translation.getTranslation())
                .isNotNull()
                .isEqualTo(THE_QUICK_FOX_PHRASE);

            verify(dictionaryRepository).findByEnglishPhrase(THE_QUICK_FOX_PHRASE);
            verifyNoMoreInteractions(dictionaryRepository);
        }

        @Test
        void testShouldTranslatePhraseWhenEnglishPhraseIsNotInDictionary() {
            final DictionaryEntity dictionaryEntity = createDictionaryEntity(THE_QUICK_FOX_PHRASE, null);
            doReturn(Optional.empty()).when(dictionaryRepository).findByEnglishPhrase(anyString());
            doReturn(dictionaryEntity).when(dictionaryRepository).saveAndFlush(dictionaryEntity);

            final Translation translation = dictionaryService.getTranslation(THE_QUICK_FOX_PHRASE);

            assertThat(translation.getTranslation())
                .isNotNull()
                .isEqualTo(THE_QUICK_FOX_PHRASE);

            verify(dictionaryRepository).saveAndFlush(any());
            verify(dictionaryRepository).findByEnglishPhrase(THE_QUICK_FOX_PHRASE);
        }

        @Test
        void testShouldReturnTranslations() {
            // GIVEN
            final String englishPhrase = "English phrase";
            final String englishPhraseWithNoTranslation = "English phrase with no translation";
            final String englishPhraseNotInDictionary = "English phrase not in dictionary";

            final Map<String, Translation> expectedTranslations =
                Map.of(englishPhrase, new Translation("Translated English phrase"),
                       englishPhraseWithNoTranslation, new Translation(englishPhraseWithNoTranslation),
                       englishPhraseNotInDictionary, new Translation(englishPhraseNotInDictionary)
                );

            final DictionaryEntity entity1 = createDictionaryEntity(englishPhrase, "Translated English phrase");
            final DictionaryEntity entity2 = createDictionaryEntity(englishPhraseWithNoTranslation, null);
            final DictionaryEntity entity3 = createDictionaryEntity(englishPhraseNotInDictionary, null);

            doReturn(Optional.of(entity1)).when(dictionaryRepository).findByEnglishPhrase(englishPhrase);
            doReturn(Optional.of(entity2)).when(dictionaryRepository)
                .findByEnglishPhrase(englishPhraseWithNoTranslation);
            doReturn(Optional.empty()).when(dictionaryRepository)
                .findByEnglishPhrase(englishPhraseNotInDictionary);
            doReturn(entity3).when(dictionaryRepository).saveAndFlush(entity3);

            final Set<String> requestPhrases = Set.of(
                englishPhrase,
                englishPhraseWithNoTranslation,
                englishPhraseNotInDictionary
            );

            // WHEN
            final Map<String, Translation> actualTranslations = dictionaryService.getTranslations(requestPhrases);

            // THEN
            assertThat(actualTranslations)
                .isNotEmpty()
                .containsAllEntriesOf(expectedTranslations);

            verify(dictionaryRepository).findByEnglishPhrase(englishPhrase);
            verify(dictionaryRepository).findByEnglishPhrase(englishPhraseWithNoTranslation);
            verify(dictionaryRepository).findByEnglishPhrase(englishPhraseNotInDictionary);
            verify(dictionaryRepository).saveAndFlush(entity3);
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
            given(securityUtils.hasRole(anyString())).willReturn(true);
            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(dictionaryMapper.createTranslationUploadEntity(anyString())).willReturn(createUploadEntity());

            // WHEN
            dictionaryService.putDictionary(dictionaryRequest);

            // THEN
            verify(dictionaryRepository, times(3)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryMapper, times(3)).modelToEntityWithTranslationUploadEntity(any(), any());
            verify(dictionaryRepository, times(3)).saveAndFlush(any());
            verify(translationUploadRepository, never()).save(any());
        }

        @Test
        void shouldPutANewDictionaryForUserWithoutManageTranslationsRole() {

            // GIVEN
            final Dictionary dictionaryRequest = getDictionaryRequestWithoutTranslationPhrases(3);
            given(securityUtils.hasRole(anyString())).willReturn(false);

            // WHEN
            dictionaryService.putDictionary(dictionaryRequest);

            // THEN
            verify(dictionaryRepository, times(3)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryMapper, times(3)).modelToEntityWithoutTranslationPhrase(any());
            verify(dictionaryRepository, times(3)).saveAndFlush(any());
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
            given(securityUtils.hasRole(anyString())).willReturn(true);
            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(dictionaryMapper.createTranslationUploadEntity(anyString())).willReturn(createUploadEntity());

            // WHEN
            dictionaryService.putDictionary(dictionaryRequest);

            // THEN
            verify(dictionaryRepository, times(1)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryRepository, times(1)).saveAndFlush(any());
            verify(translationUploadRepository, times(1)).save(any());
        }

        @Test
        void shouldUpdateADictionaryForUserWithoutManageTranslationsRole() {

            // GIVEN
            final Dictionary dictionaryRequest = getDictionaryRequestWithoutTranslationPhrases(1);
            final DictionaryEntity dictionaryEntity =
                createDictionaryEntity("english_1", "translated_1");

            given(dictionaryRepository.findByEnglishPhrase(any())).willReturn(Optional.of(dictionaryEntity));
            given(securityUtils.hasRole(anyString())).willReturn(false);

            // WHEN
            dictionaryService.putDictionary(dictionaryRequest);

            // THEN
            verify(dictionaryRepository, times(1)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryRepository, times(0)).saveAndFlush(any());
            // verify no translation uploaded entity created as no translations
            verify(dictionaryMapper, never()).createTranslationUploadEntity(anyString());
            verify(translationUploadRepository, never()).save(any());
        }

        @Test
        void shouldPutDictionaryRoleCheckForAValidUserWithManageTranslationsRole() {

            // GIVEN
            given(securityUtils.isBypassAuthCheck(XUI)).willReturn(false);
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
            given(securityUtils.isBypassAuthCheck(XUI)).willReturn(false);
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
            given(securityUtils.isBypassAuthCheck(DEFINITION_STORE)).willReturn(true);
            given(securityUtils.getServiceNameFromS2SToken(CLIENTS2S_TOKEN)).willReturn(DEFINITION_STORE);

            // WHEN / THEN
            assertDoesNotThrow(
                () -> dictionaryService.putDictionaryRoleCheck(CLIENTS2S_TOKEN)
            );
        }


        @Test
        void shouldFailPutDictionaryRoleCheckForAValidUserWithOutAnyRole() {

            // GIVEN
            given(securityUtils.isBypassAuthCheck(XUI)).willReturn(false);
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
            given(securityUtils.isBypassAuthCheck(XUI)).willReturn(false);
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
            given(securityUtils.isBypassAuthCheck(DEFINITION_STORE)).willReturn(true);
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
            given(securityUtils.isBypassAuthCheck(DEFINITION_STORE)).willReturn(true);
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


        protected static Dictionary getDictionaryRequestWithTranslationPhrases(int count) {
            final Map<String, Translation> expectedMap = new HashMap<>();
            IntStream.range(1, count + 1)
                .forEach(i -> expectedMap.put("english_" + i, new Translation("translated_" + i)));
            return new Dictionary(expectedMap);
        }

        protected static Dictionary getDictionaryRequestWithoutTranslationPhrases(int count) {
            final Map<String, Translation> expectedMap = new HashMap<>();
            IntStream.range(1, count + 1).forEach(i -> expectedMap.put("english_" + i, new Translation("")));
            return new Dictionary(expectedMap);
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

    protected static DictionaryEntity createDictionaryEntity(String phrase, String translationPhrase) {
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
