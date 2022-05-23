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
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.JUnitAssertionsShouldIncludeMessage"})
class DictionaryServiceTest {

    @Mock
    DictionaryRepository dictionaryRepository;

    @Mock
    Iterable<DictionaryEntity> repositoryResults;

    @Mock
    DictionaryMapper dictionaryMapper;
    @Mock
    SecurityUtils securityUtils;

    @InjectMocks
    DictionaryService dictionaryService;

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

        DictionaryEntity[] dictionaryEntities = { createDictionaryEntity(englishPhrase, translatedPhrase),
            createDictionaryEntity(englishPhrase, translatedPhrase),
            createDictionaryEntity(englishPhrase, translatedPhrase)};

        var spliterator = Arrays.spliterator(dictionaryEntities);
        given(repositoryResults.spliterator()).willReturn(spliterator);
        given(dictionaryRepository.findAll()).willReturn(repositoryResults);

        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class,
            () -> dictionaryService.getDictionaryContents()
        );

        assertEquals(String.format("Duplicate key %s (attempted merging values %s and %s)",
                                   englishPhrase, translatedPhrase, translatedPhrase),
                     illegalStateException.getMessage());
    }

    private DictionaryEntity createDictionaryEntity(String phrase, String translationPhrase) {
        final var dictionaryEntity = new DictionaryEntity();
        dictionaryEntity.setEnglishPhrase(phrase);
        dictionaryEntity.setTranslationPhrase(translationPhrase);
        return dictionaryEntity;
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
        assertEquals(String.format(RoleMissingException.ERROR_MESSAGE, DictionaryService.MANAGE_TRANSLATIONS_ROLE),
                     roleMissingException.getMessage());
    }

    @Nested
    @DisplayName("PutDictionary")
    class PutDictionary {
        @Test
        void shouldPutANewDictionaryForUserWithManageTranslationsRole() {
            final Dictionary dictionaryRequest = getDictionaryRequest(1, 4);
            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(securityUtils.hasRole(anyString())).willReturn(true);
            dictionaryService.putDictionary(dictionaryRequest);

            verify(dictionaryRepository, times(3)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryMapper, times(3)).modelToEntityWithTranslationUploadEntity(any(), any());
            verify(dictionaryRepository, times(3)).save(any());
        }

        @Test
        void shouldPutANewDictionaryForUserWithoutManageTranslationsRole() {
            final Dictionary dictionaryRequest = getDictionaryRequest(1, 4);
            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(securityUtils.hasRole(anyString())).willReturn(false);
            dictionaryService.putDictionary(dictionaryRequest);

            verify(dictionaryRepository, times(3)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryMapper, times(3)).modelToEntityWithoutTranslationPhrase(any());
            verify(dictionaryRepository, times(3)).save(any());
        }

        @Test
        void shouldUpdateADictionaryForUserWithManageTranslationsRole() {
            final Dictionary dictionaryRequest = getDictionaryRequest(1, 2);
            final DictionaryEntity dictionaryEntity =
                createDictionaryEntity("english_1", "translated_1");

            given(dictionaryRepository.findByEnglishPhrase(any())).willReturn(Optional.of(dictionaryEntity));

            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(securityUtils.hasRole(anyString())).willReturn(true);
            dictionaryService.putDictionary(dictionaryRequest);

            verify(dictionaryRepository, times(1)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryRepository, times(1)).save(any());
        }


        @Test
        void shouldUpdateADictionaryForUserWithoutManageTranslationsRole() {
            final Dictionary dictionaryRequest = getDictionaryRequest(1, 2);
            final DictionaryEntity dictionaryEntity =
                createDictionaryEntity("english_1", "translated_1");

            given(dictionaryRepository.findByEnglishPhrase(any())).willReturn(Optional.of(dictionaryEntity));

            given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
            given(securityUtils.hasRole(anyString())).willReturn(false);
            dictionaryService.putDictionary(dictionaryRequest);

            verify(dictionaryRepository, times(1)).findByEnglishPhrase(any());
            verify(securityUtils, times(1)).hasRole(anyString());
            verify(dictionaryRepository, times(0)).save(any());
        }


        private Dictionary getDictionaryRequest(int from, int to) {
            final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
            IntStream.range(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, "translated_" + i));
            return new Dictionary(expectedMapKeysAndValues);
        }


        private UserInfo getUserInfoWithManageTranslationsRole() {
            UserInfo userInfo = UserInfo.builder()
                .familyName("NE_NU_NE")
                .name("PEPE")
                .givenName("givenName")
                .uid("11111111")
                .roles(Arrays.asList("ROLE", "manage-translations"))
                .sub("sub")
                .build();
            return userInfo;
        }
    }
}

