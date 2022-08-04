package uk.gov.hmcts.reform.translate.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(propagation = Propagation.REQUIRED)
public interface DictionaryRepository  {

    List<DictionaryEntity> findAll();

    Optional<DictionaryEntity> findByEnglishPhrase(String englishPhrase);

    Optional<DictionaryEntity> findById(Long id);

    long deleteByEnglishPhraseStartingWith(String startingWith);

    <S extends DictionaryEntity> S saveAndFlush(S entity);

}
