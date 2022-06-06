package uk.gov.hmcts.reform.translate.repository;

import org.assertj.core.util.Streams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.translate.BaseTest;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryRepositoryIT extends BaseTest {
    @Autowired
    DictionaryRepository dictionaryRepository;

    private static final String ENGLISH_PHRASE = "English phrase";
    private static final String TRANSLATION_PHRASE = "Welsh translation phrase";
    private static final String IDAM_USER_ID = UUID.randomUUID().toString();

    @Sql(scripts = DELETE_TRANSLATION_TABLES_SCRIPT)
    @Test
    void testSaveDictionaryAndTranslationUpload() {
        final var now = LocalDateTime.now();
        final var translationUploadEntity = new TranslationUploadEntity();
        translationUploadEntity.setUploaded(now);
        translationUploadEntity.setUserId(IDAM_USER_ID);

        final var dictionaryEntity = new DictionaryEntity();
        dictionaryEntity.setEnglishPhrase(ENGLISH_PHRASE);
        dictionaryEntity.setTranslationPhrase(TRANSLATION_PHRASE);
        dictionaryEntity.setTranslationUpload(translationUploadEntity);

        final var dictionary = dictionaryRepository.save(dictionaryEntity);

        assertNotNull(dictionary.getId());

        assertAll(
            () -> assertEquals(TRANSLATION_PHRASE, dictionary.getTranslationPhrase()),
            () -> assertEquals(ENGLISH_PHRASE, dictionary.getEnglishPhrase()),
            () -> assertEquals(now, dictionary.getTranslationUpload().getUploaded()),
            () -> assertEquals(IDAM_USER_ID, dictionary.getTranslationUpload().getUserId())
        );
    }

    @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
    @Test
    void testFindDictionaryAndTranslationUploadNoTranslationPhrase() {
        final var optionalDictionaryEntity = dictionaryRepository.findById(1L);
        assertTrue(optionalDictionaryEntity.isPresent());

        final var dictionaryEntity = optionalDictionaryEntity.get();

        assertAll(
            () -> assertNull(dictionaryEntity.getTranslationPhrase()),
            () -> assertEquals("English Phrase 1", dictionaryEntity.getEnglishPhrase()),
            () -> assertEquals(
                LocalDateTime.parse("2022-05-06T11:12:13.000000"),
                dictionaryEntity.getTranslationUpload().getUploaded()
            ),
            () -> assertEquals("IdamUser1", dictionaryEntity.getTranslationUpload().getUserId())
        );
    }

    @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
    @Test
    void testFindDictionaryAndTranslationUploadWithTranslationPhrase() {
        final var optionalDictionaryEntity = dictionaryRepository.findById(2L);
        assertTrue(optionalDictionaryEntity.isPresent());

        final var dictionaryEntity = optionalDictionaryEntity.get();

        assertAll(
            () -> assertEquals(dictionaryEntity.getTranslationPhrase(), "Translated Phrase 2"),
            () -> assertEquals("English Phrase 2", dictionaryEntity.getEnglishPhrase()),
            () -> assertEquals(
                LocalDateTime.parse("2022-05-07T09:00:05.000000"),
                dictionaryEntity.getTranslationUpload().getUploaded()
            ),
            () -> assertEquals("IdamUser2", dictionaryEntity.getTranslationUpload().getUserId())
        );
    }

    @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
    @Test
    void testFindAllDictionaryAndTranslationUploadEntities() {
        final var allDictionaryEntities = dictionaryRepository.findAll();

        assertEquals(3, Streams.stream(allDictionaryEntities.iterator()).count());

        assertTrue(Streams.stream(allDictionaryEntities.iterator())
                       .allMatch(dictionaryEntity -> dictionaryEntity.getTranslationUpload() != null));
    }

    @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
    @Test
    void testShouldFindDictionaryEntityByEnglishPhrase() {
        final Optional<DictionaryEntity> optionalDictionaryEntity =
            dictionaryRepository.findByEnglishPhrase("English Phrase 1");

        assertThat(optionalDictionaryEntity)
            .isPresent()
            .map(DictionaryEntity::getEnglishPhrase)
            .hasValue("English Phrase 1");
    }

    @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
    @Test
    void testFindDictionaryEntityByEnglishPhraseShouldReturnEmptyWhenPhraseIsNotPresent() {
        final Optional<DictionaryEntity> optionalDictionaryEntity =
            dictionaryRepository.findByEnglishPhrase(ENGLISH_PHRASE);

        assertThat(optionalDictionaryEntity)
            .isNotPresent();
    }

    @Test
    @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, ADD_ENGLISH_PHRASE_SCRIPT})
    void testInsertDuplicateEnglishPhrase() {
        // GIVEN
        DictionaryEntity dictionaryEntity = new DictionaryEntity();
        dictionaryEntity.setEnglishPhrase(ENGLISH_PHRASE);

        // WHEN
        final Throwable thrown = catchThrowable(() -> dictionaryRepository.save(dictionaryEntity));

        // THEN
        assertThat(thrown)
            .isNotNull()
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
