package uk.gov.hmcts.reform.translate.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;

@Transactional(propagation = Propagation.REQUIRES_NEW)
@Repository
public interface DictionaryRepository extends CrudRepository<DictionaryEntity, Integer> {
}
