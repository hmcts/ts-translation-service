package uk.gov.hmcts.reform.translate.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.translate.ApplicationParams;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;
import uk.gov.hmcts.reform.translate.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.translate.errorhandling.RequestErrorException;
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.repository.TranslationUploadRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;


import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.BAD_SCHEMA;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.WELSH_NOT_ALLOWED;
import static uk.gov.hmcts.reform.translate.helper.DictionaryUtils.hasAnyTranslations;
import static uk.gov.hmcts.reform.translate.helper.DictionaryUtils.hasTranslationPhrase;
import static uk.gov.hmcts.reform.translate.helper.DictionaryUtils.isTranslationBodyEmpty;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.LOAD_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;

@Service
@Slf4j
public class DictionaryService {

    public static final String TEST_PHRASES_START_WITH = "TEST-";

    private final DictionaryRepository dictionaryRepository;
    private final DictionaryMapper dictionaryMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;
    private final TranslationUploadRepository translationUploadRepository;

    @Autowired
    public DictionaryService(DictionaryRepository dictionaryRepository, DictionaryMapper dictionaryMapper,
                             SecurityUtils securityUtils, ApplicationParams applicationParams,
                             TranslationUploadRepository translationUploadRepository) {

        this.dictionaryRepository = dictionaryRepository;
        this.dictionaryMapper = dictionaryMapper;
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
        this.translationUploadRepository = translationUploadRepository;
    }

    public void deleteTestPhrases() {
        // check manage-translations role before manipulating phrases that may contain translations.
        if (securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE)) {
            long deleteCount = dictionaryRepository.deleteByEnglishPhraseStartingWith(TEST_PHRASES_START_WITH);
            log.warn("User {} has deleted {} test phrases matching '{}'",
                     securityUtils.getUserId(), deleteCount, TEST_PHRASES_START_WITH);
        } else {
            throw new RoleMissingException(MANAGE_TRANSLATIONS_ROLE);
        }
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
                DictionaryEntity dictionaryEntity = new DictionaryEntity();
                dictionaryEntity.setEnglishPhrase(englishPhrase);
                return dictionaryRepository.save(dictionaryEntity);
            });

        return Optional.ofNullable(entity.getTranslationPhrase()).orElseGet(entity::getEnglishPhrase);
    }

    @Transactional
    public void putDictionary(final Dictionary dictionaryRequest) {

        val isManageTranslationRole = securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE);
        validateDictionary(dictionaryRequest, isManageTranslationRole);
        val currentUserId = securityUtils.getUserInfo().getUid();

        val translationUploadEntity = hasAnyTranslations(dictionaryRequest)
            ? dictionaryMapper.createTranslationUploadEntity(currentUserId)
            : null;

        dictionaryRequest.getTranslations().entrySet()
            .forEach(phrase -> processPhrase(phrase, translationUploadEntity));
    }

    private void processPhrase(Map.Entry<String, String> currentPhrase,
                               TranslationUploadEntity translationUploadEntity) {

        val result = dictionaryRepository.findByEnglishPhrase(currentPhrase.getKey());
        if (result.isPresent()) {
            updatePhrase(currentPhrase, result.get(), translationUploadEntity);
        } else {
            createNewPhrase(currentPhrase, translationUploadEntity);
        }
    }


    private void createNewPhrase(Map.Entry<String, String> currentPhrase,
                                 TranslationUploadEntity translationUploadOptional) {

        val newEntity = hasTranslationPhrase(currentPhrase)
            ? dictionaryMapper.modelToEntityWithTranslationUploadEntity(currentPhrase, translationUploadOptional)
            : dictionaryMapper.modelToEntityWithoutTranslationPhrase(currentPhrase);
        dictionaryRepository.save(newEntity);
    }

    private void updatePhrase(Map.Entry<String, String> currentPhrase,
                              DictionaryEntity dictionaryEntity,
                              TranslationUploadEntity translationUploadOptional) {

        if (hasTranslationPhrase(currentPhrase)) {
            dictionaryEntity.setTranslationUpload(translationUploadOptional);
            dictionaryEntity.setTranslationPhrase(currentPhrase.getValue());
            // if upload entity has been generated save it now as we know
            // we have at least one translation that will use it
            translationUploadRepository.save(translationUploadOptional);
            dictionaryRepository.save(dictionaryEntity);
        }
    }

    public void putDictionaryRoleCheck(String clientS2SToken) {
        val clientServiceName = securityUtils.getServiceNameFromS2SToken(clientS2SToken);
        if (isBypassRoleAuthCheck(clientServiceName)
            || securityUtils.hasAnyOfTheseRoles(Arrays.asList(MANAGE_TRANSLATIONS_ROLE, LOAD_TRANSLATIONS_ROLE))) {
            return;
        }
        throw new RequestErrorException(MANAGE_TRANSLATIONS_ROLE + "," + LOAD_TRANSLATIONS_ROLE);
    }

    private boolean isBypassRoleAuthCheck(String clientServiceName) {
        return applicationParams.getPutDictionaryS2sServicesBypassRoleAuthCheck().contains(clientServiceName);
    }

    private void validateDictionary(final Dictionary dictionaryRequest, boolean isManageTranslationRole) {

        if (isTranslationBodyEmpty(dictionaryRequest)) {
            throw new BadRequestException(BAD_SCHEMA);
        }
        if (!isManageTranslationRole && hasAnyTranslations(dictionaryRequest)) {
            throw new BadRequestException(WELSH_NOT_ALLOWED);
        }
    }
}
