package uk.gov.hmcts.reform.translate.service;

import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class DictionaryService {

    protected static final String MANAGE_TRANSLATIONS_ROLE = "manage-translations";
    private final DictionaryRepository dictionaryRepository;
    private final DictionaryMapper dictionaryMapper;
    private final SecurityUtils securityUtils;

    @Autowired
    public DictionaryService(DictionaryRepository dictionaryRepository, DictionaryMapper dictionaryMapper,
                             SecurityUtils securityUtils) {

        this.dictionaryRepository = dictionaryRepository;
        this.dictionaryMapper = dictionaryMapper;
        this.securityUtils = securityUtils;
    }

    public Map<String, String> getDictionaryContents() {

        if (securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE)) {
            final var dictionaryEntities = dictionaryRepository.findAll();

            final var spliterator = dictionaryEntities.spliterator();

            if (dictionaryEntities.spliterator() != null) {
                Stream<DictionaryEntity> stream = StreamSupport.stream(spliterator, false);

                return stream.collect(Collectors.toMap(
                    DictionaryEntity::getEnglishPhrase,
                    dictionaryEntity ->
                        dictionaryEntity.getTranslationPhrase() == null ? "" : dictionaryEntity.getTranslationPhrase()
                ));
            }

            return Collections.emptyMap();
        } else {
            throw new RoleMissingException(MANAGE_TRANSLATIONS_ROLE);
        }
    }

    @Transactional
    public Map<String, String> getTranslations(@NonNull final Set<String> phrases) {
        return phrases.stream()
            .map(phrase -> {
                final String translation = getTranslation(phrase);
                return Map.of(phrase, translation);
            })
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    String getTranslation(@NonNull final String englishPhrase) {
        final DictionaryEntity entity = dictionaryRepository.findByEnglishPhrase(englishPhrase)
            .orElseGet(() -> {
                final DictionaryEntity dictionaryEntity = DictionaryEntity.builder()
                    .englishPhrase(englishPhrase)
                    .build();
                return dictionaryRepository.save(dictionaryEntity);
            });

        return Optional.ofNullable(entity.getTranslationPhrase()).orElseGet(entity::getEnglishPhrase);
    }

    public void putDictionary(final Dictionary dictionaryRequest) {

        val isManageTranslationRole = securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE);
        val currentUserId = securityUtils.getUserInfo().getUid();
        dictionaryRequest.getTranslations().entrySet()
            .stream()
            .forEach(phrase -> processPhrase(phrase, currentUserId, isManageTranslationRole));
    }

    private void processPhrase(Map.Entry<String, String> currentPhrase, String currentUserId,
                               boolean isManageTranslationRole) {

        val result = dictionaryRepository.findByEnglishPhrase(currentPhrase.getKey());
        if (result.isPresent()) {
            updatePhrase(currentPhrase, currentUserId, result.get(), isManageTranslationRole);
        } else {
            createNewPhrase(currentPhrase, currentUserId, isManageTranslationRole);
        }
    }

    private void createNewPhrase(Map.Entry<String, String> currentPhrase, String currentUserId,
                                 boolean isManageTranslationRole) {

        val newEntity = isManageTranslationRole
            ? dictionaryMapper.modelToEntityWithTranslationUploadEntity(currentPhrase, currentUserId)
            : dictionaryMapper.modelToEntityWithoutTranslationPhrase(currentPhrase);
        dictionaryRepository.save(newEntity);
    }

    private void updatePhrase(Map.Entry<String, String> currentPhrase,
                              String currentUserId,
                              DictionaryEntity dictionaryEntity, boolean isManageTranslationRole) {

        if (isManageTranslationRole) {
            val translationUploadEntity = new TranslationUploadEntity();
            translationUploadEntity.setUserId(currentUserId);
            translationUploadEntity.setUploaded(LocalDateTime.now());
            dictionaryEntity.setTranslationPhrase(currentPhrase.getValue());
            dictionaryEntity.setTranslationUpload(translationUploadEntity);
            dictionaryRepository.save(dictionaryEntity);
        }
    }
}
