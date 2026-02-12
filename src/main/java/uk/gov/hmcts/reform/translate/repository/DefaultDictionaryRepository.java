package uk.gov.hmcts.reform.translate.repository;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.errorhandling.EnglishPhraseUniqueConstraintException;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Qualifier(DefaultDictionaryRepository.QUALIFIER)
@Repository
@Slf4j
@Transactional(propagation = Propagation.REQUIRED)
public class DefaultDictionaryRepository implements DictionaryRepository {

    public static final String QUALIFIER = "default";

    protected static final String ENGLISH_PHRASE_UNIQUE_CONSTRAINT = "english_phrase_unique";

    private final DictionaryRepository dictionaryRepository;

    @Autowired
    public DefaultDictionaryRepository(final @Qualifier(JpaDictionaryRepository.QUALIFIER)
                                        DictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
    }

    @Override
    public List<DictionaryEntity> findAll() {
        return dictionaryRepository.findAll();
    }

    @Override
    public List<DictionaryEntity> findAll(Pageable pageable) {
        return dictionaryRepository.findAll(pageable);
    }

    @Override
    public Optional<DictionaryEntity> findByEnglishPhrase(String englishPhrase) {
        return dictionaryRepository.findByEnglishPhrase(englishPhrase);
    }

    @Override
    public Optional<DictionaryEntity> findById(Long id) {
        return dictionaryRepository.findById(id);
    }

    @Override
    public long deleteByEnglishPhraseStartingWith(String startingWith) {
        return dictionaryRepository.deleteByEnglishPhraseStartingWith(startingWith);
    }

    @Override
    public <S extends DictionaryEntity> S saveAndFlush(S entity) {

        try {
            return dictionaryRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException
                && isDuplicateEnglishPhrase(e)) {

                log.info("CONFLICT: Failed to save phrase due to constraint violation");
                throw new EnglishPhraseUniqueConstraintException(
                    "Failed to save phrase due to constraint violation", e
                );
            } else {
                // rethrow as this is not the exception we were looking for
                throw e;
            }
        }
    }

    private boolean isDuplicateEnglishPhrase(DataIntegrityViolationException e) {
        return ((ConstraintViolationException) e.getCause()).getConstraintName()
            .equals(ENGLISH_PHRASE_UNIQUE_CONSTRAINT);
    }
}
