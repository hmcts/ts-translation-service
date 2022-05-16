package uk.gov.hmcts.reform.translate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.errorhandling.RoleMissingException;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.util.Collections;
import java.util.Map;
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

        UserInfo userInfo = securityUtils.getUserInfo();

        if (userInfo == null || !securityUtils.hasRole(MANAGE_TRANSLATIONS_ROLE, userInfo.getRoles())) {
            throw new RoleMissingException(MANAGE_TRANSLATIONS_ROLE);
        }

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
    }
}
