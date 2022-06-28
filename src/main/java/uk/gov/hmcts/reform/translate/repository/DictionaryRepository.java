package uk.gov.hmcts.reform.translate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;

import java.util.Optional;

@Transactional(propagation = Propagation.REQUIRES_NEW)
@Repository
public interface DictionaryRepository extends JpaRepository<DictionaryEntity, Long> {

    Optional<DictionaryEntity> findByEnglishPhrase(String englishPhrase);

}
