package uk.gov.hmcts.reform.translate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.JUnitAssertionsShouldIncludeMessage"})
class DictionaryServiceTest {

    @Mock
    DictionaryRepository dictionaryRepository;

    @Mock
    Iterable<DictionaryEntity> repositoryResults;

    @Mock
    SecurityUtils securityUtils;

    @InjectMocks
    DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        given(securityUtils.hasRole(any())).willReturn(true);
    }

    private final String THE_QUICK_FOX_PHRASE = "the quick fox";

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
        return DictionaryEntity.builder()
            .englishPhrase(phrase)
            .translationPhrase(translationPhrase)
            .build();
    }

    @Test
    void shouldReturnEmptyDictionaryContents() {
        given(dictionaryRepository.findAll()).willReturn(repositoryResults);
        assertTrue(dictionaryService.getDictionaryContents().isEmpty());
    }

    @Test
    void testShouldTranslatePhrase() {
        final DictionaryEntity dictionaryEntity = createDictionaryEntity(THE_QUICK_FOX_PHRASE, "translated");
        doReturn(Optional.of(dictionaryEntity)).when(dictionaryRepository).findByEnglishPhrase(anyString());

        final String translation = dictionaryService.getTranslation(THE_QUICK_FOX_PHRASE);

        assertThat(translation)
            .isNotNull()
            .isEqualTo("translated");

        verify(dictionaryRepository).findByEnglishPhrase(eq(THE_QUICK_FOX_PHRASE));
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

        verify(dictionaryRepository).findByEnglishPhrase(eq(THE_QUICK_FOX_PHRASE));
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
        verify(dictionaryRepository).findByEnglishPhrase(eq(THE_QUICK_FOX_PHRASE));
    }

    @Test
    void testShouldReturnTranslations() {
        // GIVEN
        final Map<String, String> expectedTranslations =
            Map.of("English phrase", "Translated English phrase",
                   "English phrase with no translation", "English phrase with no translation",
                   "English phrase not in dictionary", "English phrase not in dictionary");

        final DictionaryEntity entity1 = createDictionaryEntity("English phrase", "Translated English phrase");
        final DictionaryEntity entity2 = createDictionaryEntity("English phrase with no translation", null);
        final DictionaryEntity entity3 = createDictionaryEntity("English phrase not in dictionary", null);

        doReturn(Optional.of(entity1)).when(dictionaryRepository).findByEnglishPhrase(eq("English phrase"));
        doReturn(Optional.of(entity2)).when(dictionaryRepository)
            .findByEnglishPhrase(eq("English phrase with no translation"));
        doReturn(Optional.empty()).when(dictionaryRepository).findByEnglishPhrase(eq("English phrase not in dictionary"));
        doReturn(entity3).when(dictionaryRepository).save(eq(entity3));

        final List<String> translationRequestPhrases = List.of(
            "English phrase",
            "English phrase with no translation",
            "English phrase not in dictionary"
        );

        // WHEN
        final Map<String, String> actualTranslations = dictionaryService.getTranslations(translationRequestPhrases);

        // THEN
        assertThat(actualTranslations)
            .isNotEmpty()
            .containsAllEntriesOf(expectedTranslations);

        verify(dictionaryRepository).findByEnglishPhrase(eq("English phrase"));
        verify(dictionaryRepository).findByEnglishPhrase(eq("English phrase with no translation"));
        verify(dictionaryRepository).findByEnglishPhrase(eq("English phrase not in dictionary"));
        verify(dictionaryRepository).save(eq(entity3));
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
}

