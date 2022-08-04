package uk.gov.hmcts.reform.translate.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;

@Qualifier(JpaDictionaryRepository.QUALIFIER)
@Repository
@Transactional(propagation = Propagation.REQUIRED)
public interface JpaDictionaryRepository extends JpaRepository<DictionaryEntity, Long>, DictionaryRepository {
    String QUALIFIER = "jpa";

    @Override
    @SuppressWarnings("NullableProblems")
    <S extends DictionaryEntity>  S saveAndFlush(S entity);

}
