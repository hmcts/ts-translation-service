package uk.gov.hmcts.reform.translate.service;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.DictionaryRequest;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;
import uk.gov.hmcts.reform.translate.security.UserInfo;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class DictionaryService {

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
        final var dictionaryEntities = dictionaryRepository.findAll();

        final var spliterator = dictionaryEntities.spliterator();

        Map<String, String> returnValue = Collections.emptyMap();

        if (dictionaryEntities.spliterator() != null) {
            Stream<DictionaryEntity> stream = StreamSupport.stream(spliterator, false);

            returnValue = stream.collect(Collectors.toMap(
                DictionaryEntity::getEnglishPhrase,
                dictionaryEntity ->
                    dictionaryEntity.getTranslationPhrase() == null ? "" : dictionaryEntity.getTranslationPhrase()
            ));
        }

        return returnValue;
    }


    public void putDictionary(final DictionaryRequest dictionaryRequest) {

        val currentUser = securityUtils.getUserInfo();
        dictionaryRequest.getTranslations().entrySet()
            .stream()
            .forEach(englishPhrase -> processEachPhrase(englishPhrase, currentUser));
    }

    private void processEachPhrase(Map.Entry<String, String> currentPhrase, UserInfo currentUser) {
        val result = dictionaryRepository.findByEnglishPhrase(currentPhrase.getKey());
        if (result.isPresent()) {
            updatePhrase(currentPhrase, currentUser, result.get());
        } else {
            createNewPhrase(currentPhrase, currentUser);
        }
    }

    private void createNewPhrase(Map.Entry<String, String> currentPhrase, UserInfo currentUser) {

        val newEntity = isCurrentRole(currentUser)
            ? dictionaryMapper.modelToEntityWithTranslationUploadEntity(currentPhrase, currentUser.getUid())
            : dictionaryMapper.modelToEntityWithoutTranslationPhrase(currentPhrase);
        dictionaryRepository.save(newEntity);
    }

    private void updatePhrase(Map.Entry<String, String> currentPhrase,
                              UserInfo currentUser, DictionaryEntity dictionaryEntity) {
        if (isCurrentRole(currentUser)) {
            val translationUploadEntity = new TranslationUploadEntity();
            translationUploadEntity.setUserId(currentUser.getUid());
            translationUploadEntity.setUploaded(LocalDateTime.now());
            dictionaryEntity.setTranslationPhrase(currentPhrase.getValue());
            dictionaryEntity.setTranslationUpload(translationUploadEntity);
            dictionaryRepository.save(dictionaryEntity);
        }
    }

    private boolean isCurrentRole(UserInfo currentUser) {
        return securityUtils.hasManageTranslationsRole(currentUser.getRoles());
    }
}
