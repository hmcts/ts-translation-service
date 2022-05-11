package uk.gov.hmcts.reform.translate.service;

import groovy.lang.IntRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

        new IntRange(1, 3).forEach(i -> expectedMapKeysAndValues.put("english" + i, "translated" + i));

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

    private DictionaryEntity createDictionaryEntity(String phrase, String translationPhrase) {
        final var dictionaryEntity = new DictionaryEntity();
        dictionaryEntity.setEnglishPhrase(phrase);
        dictionaryEntity.setEnglishPhrase(translationPhrase);
        return dictionaryEntity;
    }

    @Test
    void shouldReturnEmptyDictionaryContents() {
        given(dictionaryRepository.findAll()).willReturn(repositoryResults);
        assertTrue(dictionaryService.getDictionaryContents().isEmpty());
    }
}

