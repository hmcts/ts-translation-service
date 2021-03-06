package uk.gov.hmcts.reform.translate.helper;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DictionaryMapperTest")
class DictionaryMapperTest {

    private static final String ENGLISH = "english";
    private static final String WELSH = "welsh";

    @Test
    void shouldMapModelToEntityWithTranslationUploadEntity() {
        val dictionaryMapper = new DictionaryMapper();
        val currentPhrase = getCurrentPhrase();
        val translationUploadEntity = new TranslationUploadEntity();
        translationUploadEntity.setUserId("3333");
        val dictionaryEntity = dictionaryMapper.modelToEntityWithTranslationUploadEntity(
            currentPhrase,
            translationUploadEntity
        );
        assertEquals(ENGLISH, dictionaryEntity.getEnglishPhrase());
        assertEquals(WELSH, dictionaryEntity.getTranslationPhrase());
        assertTrue(dictionaryEntity.getTranslationUpload() != null);
    }

    @Test
    void shouldMapModelToEntity() {
        val dictionaryMapper = new DictionaryMapper();
        val currentPhrase = getCurrentPhrase();
        val dictionaryEntity = dictionaryMapper.modelToEntity(currentPhrase);
        assertEquals(ENGLISH, dictionaryEntity.getEnglishPhrase());
        assertEquals(WELSH, dictionaryEntity.getTranslationPhrase());
        assertTrue(dictionaryEntity.getTranslationUpload() == null);
    }

    @Test
    void shouldMaModelToEntityWithoutTranslationPhrase() {

        val dictionaryMapper = new DictionaryMapper();
        val currentPhrase = getCurrentPhrase();
        val dictionaryEntity = dictionaryMapper.modelToEntityWithoutTranslationPhrase(currentPhrase);
        assertEquals(ENGLISH, dictionaryEntity.getEnglishPhrase());
        assertEquals(null, dictionaryEntity.getTranslationPhrase());
        assertTrue(dictionaryEntity.getTranslationUpload() == null);
    }

    private Map.Entry<String, String> getCurrentPhrase() {

        Map<String, String> currentPhrase = new HashMap<>();
        currentPhrase.put(ENGLISH, WELSH);
        return currentPhrase.entrySet().iterator().next();
    }
}
