package uk.gov.hmcts.reform.translate.helper;

import lombok.val;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class DictionaryMapper {

    public DictionaryEntity modelToEntityWithTranslationUploadEntity(final Map.Entry<String, String> currentPhrase,
                                                                     final String currentUserId) {

        val dictionaryEntity = modelToEntity(currentPhrase);
        val translationUploadEntity = new TranslationUploadEntity();
        translationUploadEntity.setUserId(currentUserId);
        translationUploadEntity.setUploaded(LocalDateTime.now());

        dictionaryEntity.setTranslationUpload(translationUploadEntity);
        return dictionaryEntity;
    }

    public DictionaryEntity modelToEntity(final Map.Entry<String, String> currentPhrase) {
        return DictionaryEntity.builder()
            .englishPhrase(currentPhrase.getKey())
            .translationPhrase(currentPhrase.getValue())
            .build();
    }

    public DictionaryEntity modelToEntityWithoutTranslationPhrase(final Map.Entry<String, String> currentPhrase) {
        return DictionaryEntity.builder()
            .englishPhrase(currentPhrase.getKey())
            .build();
    }
}
