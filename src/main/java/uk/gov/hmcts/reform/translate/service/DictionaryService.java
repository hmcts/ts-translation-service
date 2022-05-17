package uk.gov.hmcts.reform.translate.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class DictionaryService {

    protected static final String MANAGE_TRANSLATIONS_ROLE = "manage-translations";

    private final DictionaryRepository dictionaryRepository;

    private final SecurityUtils securityUtils;

    @Autowired
    public DictionaryService(DictionaryRepository dictionaryRepository, SecurityUtils securityUtils) {
        this.dictionaryRepository = dictionaryRepository;
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

    public Map<String, String> getTranslations(@NonNull final List<String> phrases) {
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
}
