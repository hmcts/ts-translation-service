package uk.gov.hmcts.reform.translate.repository;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.errorhandling.EnglishPhraseUniqueConstraintException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.translate.repository.DefaultDictionaryRepository.ENGLISH_PHRASE_UNIQUE_CONSTRAINT;

@DisplayName("DefaultDictionaryRepository")
@ExtendWith(MockitoExtension.class)
class DefaultDictionaryRepositoryTest {

    private static final String ENGLISH_PHRASE = "English phrase";

    @Mock
    private DictionaryRepository dictionaryRepository;

    @InjectMocks
    private DefaultDictionaryRepository underTest;

    private final DictionaryEntity dictionaryEntity = new DictionaryEntity();

    @Test
    @DisplayName("should call decorated operation: findAll")
    void shouldCallDecoratedOperation_findAll() {

        // GIVEN
        when(dictionaryRepository.findAll()).thenReturn(List.of(dictionaryEntity));

        // WHEN
        var response = underTest.findAll();

        // THEN
        verify(dictionaryRepository).findAll();
        assertEquals(dictionaryEntity, response.get(0));
    }

    @Test
    @DisplayName("should call decorated operation: findAll(Pageable)")
    void shouldCallDecoratedOperation_findAllPageable() {

        // GIVEN
        var pageable = PageRequest.of(0, 5);
        var page = new PageImpl<>(List.of(dictionaryEntity), pageable, 1);
        when(dictionaryRepository.findAll(pageable)).thenReturn(page);

        // WHEN
        var response = underTest.findAll(pageable);

        // THEN
        verify(dictionaryRepository).findAll(pageable);
        assertEquals(dictionaryEntity, response.getContent().get(0));
    }

    @Test
    @DisplayName("should call decorated operation: findByEnglishPhrase")
    void shouldCallDecoratedOperation_findByEnglishPhrase() {

        // GIVEN
        when(dictionaryRepository.findByEnglishPhrase(ENGLISH_PHRASE)).thenReturn(Optional.of(dictionaryEntity));

        // WHEN
        var response = underTest.findByEnglishPhrase(ENGLISH_PHRASE);

        // THEN
        verify(dictionaryRepository).findByEnglishPhrase(ENGLISH_PHRASE);
        assertEquals(dictionaryEntity, response.orElse(null));
    }

    @Test
    @DisplayName("should call decorated operation: findById")
    void shouldCallDecoratedOperation_findById() {

        // GIVEN
        when(dictionaryRepository.findById(1234L)).thenReturn(Optional.of(dictionaryEntity));

        // WHEN
        var response = underTest.findById(1234L);

        // THEN
        verify(dictionaryRepository).findById(1234L);
        assertEquals(dictionaryEntity, response.orElse(null));
    }

    @Test
    @DisplayName("should call decorated operation: deleteByEnglishPhraseStartingWith")
    void shouldCallDecoratedOperation_deleteByEnglishPhraseStartingWith() {

        // WHEN
        underTest.deleteByEnglishPhraseStartingWith(ENGLISH_PHRASE);

        // THEN
        verify(dictionaryRepository).deleteByEnglishPhraseStartingWith(ENGLISH_PHRASE);
    }

    @Test
    @DisplayName("should call decorated operation: saveAndFlush")
    void shouldCallDecoratedOperation_saveAndFlush() {

        // GIVEN
        when(dictionaryRepository.saveAndFlush(dictionaryEntity)).thenReturn(dictionaryEntity);

        // WHEN
        var response = underTest.saveAndFlush(dictionaryEntity);

        // THEN
        verify(dictionaryRepository).saveAndFlush(dictionaryEntity);
        assertEquals(dictionaryEntity, response);
    }

    @Test
    @DisplayName("should throw EnglishPhraseUniqueConstraintException if saveAndFlush hits recognised constraint")
    void shouldThrowEnglishPhraseUniqueConstraintExceptionIfRecognisedConstraintException() {

        // GIVEN
        var constraintException = new ConstraintViolationException(
            "Oops", new SQLException(), ENGLISH_PHRASE_UNIQUE_CONSTRAINT
        );
        var dataIntegrityViolationException = new DataIntegrityViolationException("Exception", constraintException);
        when(dictionaryRepository.saveAndFlush(dictionaryEntity)).thenThrow(dataIntegrityViolationException);

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.saveAndFlush(dictionaryEntity));

        // THEN
        assertThat(thrown)
            .isNotNull()
            .isInstanceOf(EnglishPhraseUniqueConstraintException.class);
    }

    @Test
    @DisplayName("should rethrow DataIntegrityViolationException if saveAndFlush hits unrecognised constraint")
    void shouldRethrowDataIntegrityViolationExceptionIfUnrecognisedConstraintException() {

        // GIVEN
        var constraintException = new ConstraintViolationException(
            "Oops", new SQLException(), "a different constraint"
        );
        var dataIntegrityViolationException = new DataIntegrityViolationException("Exception", constraintException);
        when(dictionaryRepository.saveAndFlush(dictionaryEntity)).thenThrow(dataIntegrityViolationException);

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.saveAndFlush(dictionaryEntity));

        // THEN
        assertThat(thrown)
            .isNotNull()
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should rethrow DataIntegrityViolationException if saveAndFlush hits a different violation")
    void shouldRethrowDataIntegrityViolationExceptionIfNotAConstraintException() {

        // GIVEN
        var dataIntegrityViolationException = new DataIntegrityViolationException("Oops");
        when(dictionaryRepository.saveAndFlush(dictionaryEntity)).thenThrow(dataIntegrityViolationException);

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.saveAndFlush(dictionaryEntity));

        // THEN
        assertThat(thrown)
            .isNotNull()
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
