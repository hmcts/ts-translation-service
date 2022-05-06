package uk.gov.hmcts.reform.translate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class DictionaryService {

    private final DictionaryRepository dictionaryRepository;

    @Autowired
    public DictionaryService(DictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
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
}
