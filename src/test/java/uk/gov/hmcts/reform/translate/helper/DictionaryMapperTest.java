package uk.gov.hmcts.reform.translate.helper;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DictionaryMapperTest")
public class DictionaryMapperTest {

    private static final String ENGLISH = "english";
    private static final String WELSH = "welsh";

    @Test
    void shouldMapModelToEntityWithTranslationUploadEntity() {
        val dictionaryMapper = new DictionaryMapper();
        val currentPhrase = getCurrentPhrase();
        val dictionaryEntity = dictionaryMapper.modelToEntityWithTranslationUploadEntity(
            currentPhrase,
            "3333"
        );
        assertEquals(dictionaryEntity.getEnglishPhrase(), ENGLISH);
        assertEquals(dictionaryEntity.getTranslationPhrase(), WELSH);
        assertTrue(dictionaryEntity.getTranslationUpload() != null);
    }

    @Test
    void shouldMapModelToEntity() {
        val dictionaryMapper = new DictionaryMapper();
        val currentPhrase = getCurrentPhrase();
        val dictionaryEntity = dictionaryMapper.modelToEntity(currentPhrase);

        assertEquals(dictionaryEntity.getEnglishPhrase(), ENGLISH);
        assertEquals(dictionaryEntity.getTranslationPhrase(), WELSH);
        assertTrue(dictionaryEntity.getTranslationUpload() == null);
    }

    @Test
    void shouldMaModelToEntityWithoutTranslationPhrase() {

        val dictionaryMapper = new DictionaryMapper();
        val currentPhrase = getCurrentPhrase();
        val dictionaryEntity = dictionaryMapper.modelToEntityWithoutTranslationPhrase(currentPhrase);
        assertEquals(dictionaryEntity.getEnglishPhrase(), ENGLISH);
        assertEquals(dictionaryEntity.getTranslationPhrase(), null);
        assertTrue(dictionaryEntity.getTranslationUpload() == null);
    }

    private Map.Entry<String, String> getCurrentPhrase() {

        Map<String, String> currentPhrase = new HashMap<>();
        currentPhrase.put(ENGLISH, WELSH);
        return currentPhrase.entrySet().iterator().next();
    }
}
