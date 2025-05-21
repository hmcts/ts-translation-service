package uk.gov.hmcts.reform.translate.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("DictionaryMapperTest")
class DictionaryMapperTest {

    private static final String ENGLISH = "english";
    private static final String WELSH = "welsh";

    @Test
    void shouldMapModelToEntityWithTranslationUploadEntity() {
        final var dictionaryMapper = new DictionaryMapper();
        final var currentPhrase = getCurrentPhrase();
        final var translationUploadEntity = new TranslationUploadEntity();
        translationUploadEntity.setUserId("3333");
        final var dictionaryEntity = dictionaryMapper.modelToEntityWithTranslationUploadEntity(
            currentPhrase,
            translationUploadEntity
        );
        assertEquals(ENGLISH, dictionaryEntity.getEnglishPhrase());
        assertEquals(WELSH, dictionaryEntity.getTranslationPhrase());
        assertNotNull(dictionaryEntity.getTranslationUpload());
    }

    @SuppressWarnings("ConstantConditions") // NB: testing @NonNull annotation is present and active
    @Test
    void shouldThrowNullErrorIfModelToEntityWithTranslationUploadEntityIsPassedANullUploadEntity() {

        // GIVEN
        final var dictionaryMapper = new DictionaryMapper();
        final var currentPhrase = getCurrentPhrase();

        // WHEN / THEN
        assertThrows(
            NullPointerException.class,
            () -> dictionaryMapper.modelToEntityWithTranslationUploadEntity(currentPhrase, null)
        );
    }

    @Test
    void shouldMapModelToEntity() {
        final var dictionaryMapper = new DictionaryMapper();
        final var currentPhrase = getCurrentPhrase();
        final var dictionaryEntity = dictionaryMapper.modelToEntity(currentPhrase);
        assertEquals(ENGLISH, dictionaryEntity.getEnglishPhrase());
        assertEquals(WELSH, dictionaryEntity.getTranslationPhrase());
        assertNull(dictionaryEntity.getTranslationUpload());
    }

    @Test
    void shouldMapModelToEntityWithoutTranslationPhrase() {

        final var dictionaryMapper = new DictionaryMapper();
        final var currentPhrase = getCurrentPhrase();
        final var dictionaryEntity = dictionaryMapper.modelToEntityWithoutTranslationPhrase(currentPhrase);
        assertEquals(ENGLISH, dictionaryEntity.getEnglishPhrase());
        assertNull(dictionaryEntity.getTranslationPhrase());
        assertNull(dictionaryEntity.getTranslationUpload());
    }

    private Map.Entry<String, Translation> getCurrentPhrase() {

        Map<String, Translation> currentPhrase = new HashMap<>();
        currentPhrase.put(ENGLISH, new Translation(WELSH));
        return currentPhrase.entrySet().iterator().next();
    }

}
