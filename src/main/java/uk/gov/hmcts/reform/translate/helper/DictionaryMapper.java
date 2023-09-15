package uk.gov.hmcts.reform.translate.helper;

import lombok.NonNull;
import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;
import uk.gov.hmcts.reform.translate.model.Translation;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class DictionaryMapper {

    public DictionaryEntity modelToEntityWithTranslationUploadEntity(final Map.Entry<String, Translation> currentPhrase,
                                                                     @NonNull TranslationUploadEntity uploadEntity) {
        val dictionaryEntity = modelToEntity(currentPhrase);
        dictionaryEntity.setTranslationUpload(uploadEntity);
        return dictionaryEntity;
    }

    public DictionaryEntity modelToEntity(final Map.Entry<String, Translation> currentPhrase) {
        val dictionaryEntity = modelToEntityWithoutTranslationPhrase(currentPhrase);
        Translation translation = currentPhrase.getValue();
        dictionaryEntity.setTranslationPhrase(translation.getTranslation());
        if (translation.isYesOrNo()) {
            dictionaryEntity.setYesOrNo(true);
            dictionaryEntity.setYes(translation.getYes());
            dictionaryEntity.setNo(translation.getNo());
        }
        return dictionaryEntity;
    }

    public DictionaryEntity  modelToEntityWithoutTranslationPhrase(final Map.Entry<String, Translation> currentPhrase) {
        val dictionaryEntity = new DictionaryEntity();
        dictionaryEntity.setEnglishPhrase(currentPhrase.getKey());
        if (currentPhrase.getValue() != null && currentPhrase.getValue().isYesOrNo()) {
            dictionaryEntity.setYesOrNo(true);
        }
        return dictionaryEntity;
    }

    public TranslationUploadEntity createTranslationUploadEntity(String currentUserId) {
        val translationUploadEntity = new TranslationUploadEntity();
        translationUploadEntity.setUserId(currentUserId);
        translationUploadEntity.setUploaded(LocalDateTime.now());
        return translationUploadEntity;
    }
}
