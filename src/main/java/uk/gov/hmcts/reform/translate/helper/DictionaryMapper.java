package uk.gov.hmcts.reform.translate.helper;

import lombok.NonNull;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class DictionaryMapper {

    public DictionaryEntity modelToEntityWithTranslationUploadEntity(final Map.Entry<String, String> currentPhrase,
                                                                     @NonNull TranslationUploadEntity uploadEntity) {
        val dictionaryEntity = modelToEntity(currentPhrase);
        dictionaryEntity.setTranslationUpload(uploadEntity);
        return dictionaryEntity;
    }

    public DictionaryEntity modelToEntity(final Map.Entry<String, String> currentPhrase) {
        val dictionaryEntity = modelToEntityWithoutTranslationPhrase(currentPhrase);
        dictionaryEntity.setTranslationPhrase(currentPhrase.getValue());
        return dictionaryEntity;
    }

    public DictionaryEntity modelToEntityWithoutTranslationPhrase(final Map.Entry<String, String> currentPhrase) {
        val dictionaryEntity = new DictionaryEntity();
        dictionaryEntity.setEnglishPhrase(currentPhrase.getKey());
        return dictionaryEntity;
    }

    public TranslationUploadEntity createTranslationUploadEntity(String currentUserId) {
        val translationUploadEntity = new TranslationUploadEntity();
        translationUploadEntity.setUserId(currentUserId);
        translationUploadEntity.setUploaded(LocalDateTime.now());
        return translationUploadEntity;
    }
}
