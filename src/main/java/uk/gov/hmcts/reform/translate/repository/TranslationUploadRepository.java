package uk.gov.hmcts.reform.translate.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;

@Transactional(propagation = Propagation.REQUIRES_NEW)
@Repository
public interface TranslationUploadRepository extends CrudRepository<TranslationUploadEntity, Long> {
}
