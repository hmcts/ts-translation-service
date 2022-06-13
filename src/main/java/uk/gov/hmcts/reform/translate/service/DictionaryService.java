package uk.gov.hmcts.reform.translate.service;

import lombok.NonNull;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.reform.translate.security.SecurityUtils.LOAD_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;

@Service
public class DictionaryService {

    public static final String INVALID_PAYLOAD_FORMAT = "The translations field cannot be empty.";
    public static final String INVALID_PAYLOAD_FOR_ROLE = "User with a role different to "
        + MANAGE_TRANSLATIONS_ROLE + " should not have translations.";

    private final DictionaryRepository dictionaryRepository;
    private final DictionaryMapper dictionaryMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;
    private final Predicate<String> isTranslationNotNull = translation -> !StringUtils.isEmpty(translation);

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


    public void putDictionary(Dictionary dictionaryRequest) {

        val isManageTranslationRole = securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE);
        validateDictionary(dictionaryRequest, isManageTranslationRole);
        val currentUserId = securityUtils.getUserInfo().getUid();

        val translationUploadOptional = hasAnyTranslations(dictionaryRequest)
            ? Optional.of(dictionaryMapper.createTranslationUploadEntity(currentUserId))
            : dictionaryMapper.getTranslationUploadEntityEmpty();

        dictionaryRequest.getTranslations().entrySet()
            .stream()
            .forEach(phrase -> processPhrase(phrase, translationUploadOptional));
    }

    private void processPhrase(Map.Entry<String, String> currentPhrase,
                               Optional<TranslationUploadEntity> translationUploadOptional) {

        val result = dictionaryRepository.findByEnglishPhrase(currentPhrase.getKey());
        if (result.isPresent()) {
            updatePhrase(currentPhrase, result.get(), translationUploadOptional);
        } else {
            createNewPhrase(currentPhrase, translationUploadOptional);
        }
    }


    private void createNewPhrase(Map.Entry<String, String> currentPhrase,
                                 Optional<TranslationUploadEntity> translationUploadOptional) {

        val newEntity = hasAnyTranslation(currentPhrase)
            ? dictionaryMapper.modelToEntityWithTranslationUploadEntity(currentPhrase, translationUploadOptional.get())
            : dictionaryMapper.modelToEntityWithoutTranslationPhrase(currentPhrase);
        dictionaryRepository.save(newEntity);
    }

    private void updatePhrase(Map.Entry<String, String> currentPhrase,
                              DictionaryEntity dictionaryEntity,
                              Optional<TranslationUploadEntity> translationUploadOptional) {

        if (hasAnyTranslation(currentPhrase)) {
            dictionaryEntity.setTranslationUpload(translationUploadOptional.get());
            dictionaryEntity.setTranslationPhrase(currentPhrase.getValue());
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
            throw new BadRequestException(INVALID_PAYLOAD_FORMAT);
        }
        if (!isManageTranslationRole && hasAnyTranslations(dictionaryRequest)) {
            throw new BadRequestException(INVALID_PAYLOAD_FOR_ROLE);
        }
    }

    private boolean isTranslationBodyEmpty(final Dictionary dictionaryRequest) {
        return dictionaryRequest.getTranslations() == null || dictionaryRequest.getTranslations().isEmpty();
    }

    private boolean hasAnyTranslations(final Dictionary dictionaryRequest) {
        return dictionaryRequest.getTranslations().values().stream().anyMatch(isTranslationNotNull);
    }

    private boolean hasAnyTranslation(final Map.Entry<String, String> currentPhrase) {
        return isTranslationNotNull.test(currentPhrase.getValue());
    }
}
