package uk.gov.hmcts.reform.translate.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.JUnitAssertionsShouldIncludeMessage"})
class DictionaryServiceTest {

    @Mock
    DictionaryRepository dictionaryRepository;

    @Mock
    Iterable<DictionaryEntity> repositoryResults;

    @InjectMocks
    DictionaryService dictionaryService;

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
}
