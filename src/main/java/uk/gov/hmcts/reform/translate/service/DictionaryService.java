package uk.gov.hmcts.reform.translate.service;

import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.translate.ApplicationParams;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;
import uk.gov.hmcts.reform.translate.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.translate.errorhandling.RequestErrorException;
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class DictionaryService {

    protected static final String MANAGE_TRANSLATIONS_ROLE = "manage-translations";
    protected static final String LOAD_TRANSLATIONS = "load-translations";
    private final DictionaryRepository dictionaryRepository;
    private final DictionaryMapper dictionaryMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;
    private final Predicate<String> isTranslationNull = translation -> !StringUtils.isEmpty(translation);

    @Autowired
    public DictionaryService(DictionaryRepository dictionaryRepository, DictionaryMapper dictionaryMapper,
                             SecurityUtils securityUtils, ApplicationParams applicationParams) {

        this.dictionaryRepository = dictionaryRepository;
        this.dictionaryMapper = dictionaryMapper;
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
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


    public void putDictionary(Dictionary dictionaryRequest, String clientS2SToken) {

        val clientServiceName = securityUtils.getServiceNameFromS2SToken(clientS2SToken);
        validateServiceRequest(clientServiceName);
        val isADefinitionStoreCall = isADefinitionStoreCall(clientServiceName);
        val isManageTranslationRole = securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE);
        validateDictionary(dictionaryRequest, isManageTranslationRole);
        val currentUserId = securityUtils.getUserInfo().getUid();

        dictionaryRequest.getTranslations().entrySet()
            .stream()
            .forEach(phrase -> processPhrase(phrase, currentUserId, isManageTranslationRole));
    }

    private void validateServiceRequest(String clientServiceName) {
        if (isADefinitionStoreCall(clientServiceName)
            || securityUtils.hasAnyOfThisRoles(Arrays.asList(MANAGE_TRANSLATIONS_ROLE, LOAD_TRANSLATIONS))) {
            return;
        }
        throw new RequestErrorException(MANAGE_TRANSLATIONS_ROLE + "," + LOAD_TRANSLATIONS);
    }

    private boolean isADefinitionStoreCall(String clientServiceName) {
        return applicationParams.getAuthorisedServicesForTranslation().contains(clientServiceName);
    }

    private void validateDictionary(final Dictionary dictionaryRequest, boolean isManageTranslationRole) {

        if (isTranslationEmpty(dictionaryRequest)) {
            throw new BadRequestException("The translations field cannot be empty.");
        }
        if (!isManageTranslationRole && hasAnyTranslations(dictionaryRequest)) {
            val message = "User with a role different to "
                + MANAGE_TRANSLATIONS_ROLE + " should not have translations.";
            throw new BadRequestException(message);
        }
    }

    private boolean isTranslationEmpty(final Dictionary dictionaryRequest) {
        return dictionaryRequest.getTranslations() == null || dictionaryRequest.getTranslations().isEmpty();
    }

    private boolean hasAnyTranslations(final Dictionary dictionaryRequest) {
        return dictionaryRequest.getTranslations().values().stream().anyMatch(isTranslationNull);
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
