package uk.gov.hmcts.reform.translate.repository;

import org.assertj.core.util.Streams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.translate.BaseTest;
import uk.gov.hmcts.reform.translate.data.TranslationUploadEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationUploadRepositoryIT extends BaseTest {
    private static final String IDAM_USER_ID = UUID.randomUUID().toString();

    @Autowired
    TranslationUploadRepository translationUploadRepository;

    @Sql(scripts = DELETE_TRANSLATION_TABLES_SCRIPT)
    @Test
    void testSaveTranslationUpload() {
        final var now = LocalDateTime.now();
        final var translationUploadEntity = new TranslationUploadEntity();
        translationUploadEntity.setUploaded(now);
        translationUploadEntity.setUserId(IDAM_USER_ID);

        var savedTranslationUpload = translationUploadRepository.save(translationUploadEntity);

        assertNotNull(savedTranslationUpload);
    }

    @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
    @Test
    void testFindTranslationUploadById() {
        final var optionalTranslationUploadEntity = translationUploadRepository.findById(1);
        assertTrue(optionalTranslationUploadEntity.isPresent());

        final var translationUploadEntity = optionalTranslationUploadEntity.get();

        assertAll(
            () -> assertEquals(LocalDateTime.parse("2022-05-06T11:12:13.000000"),
                               translationUploadEntity.getUploaded()),
            () -> assertEquals("IdamUser1", translationUploadEntity.getUserId())
        );
    }

    @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, GET_TRANSLATION_TABLES_SCRIPT})
    @Test
    void testFindAllTranslationUploadEntities() {
        final var translationUploadEntityIterable = translationUploadRepository.findAll();

        assertEquals(3, Streams.stream(translationUploadEntityIterable.iterator()).count());

        assertTrue(Streams.stream(translationUploadEntityIterable.iterator()).allMatch(translationUploadEntity ->
            translationUploadEntity.getUploaded() != null && translationUploadEntity.getUserId() != null)
        );
    }
}
