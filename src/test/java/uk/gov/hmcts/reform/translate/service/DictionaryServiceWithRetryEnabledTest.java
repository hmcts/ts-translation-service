package uk.gov.hmcts.reform.translate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.errorhandling.EnglishPhraseUniqueConstraintException;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.repository.DefaultDictionaryRepository;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.repository.TranslationUploadRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.translate.service.DictionaryServiceTest.PutDictionary.getDictionaryRequestWithoutTranslationPhrases;
import static uk.gov.hmcts.reform.translate.service.DictionaryServiceTest.createDictionaryEntity;

@DisplayName("DictionaryService test with RetryEnabled")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DictionaryService.class, DictionaryServiceWithRetryEnabledTest.RetryConfig.class})
class DictionaryServiceWithRetryEnabledTest {

    @Autowired
    private DictionaryService dictionaryService;

    @MockBean
    @Qualifier(DefaultDictionaryRepository.QUALIFIER)
    DictionaryRepository dictionaryRepository;

    @MockBean
    @SuppressWarnings("unused")
    TranslationUploadRepository translationUploadRepository;

    @MockBean
    @SuppressWarnings("unused")
    DictionaryMapper dictionaryMapper;

    @MockBean
    SecurityUtils securityUtils;

    @Configuration
    @EnableRetry
    public static class RetryConfig {
        // spring config to enable retry for SpringRunner tests
    }

    private static final String THE_QUICK_FOX_PHRASE = "the quick fox";

    @BeforeEach
    void setUp() {

        final DictionaryEntity dictionaryEntity = createDictionaryEntity(THE_QUICK_FOX_PHRASE, null);

        // fail twice return on third
        given(dictionaryRepository.saveAndFlush(any()))
            .willThrow(new EnglishPhraseUniqueConstraintException("", null)) // 1st error
            .willThrow(new EnglishPhraseUniqueConstraintException("", null)) // 2nd error
            .willReturn(dictionaryEntity); // worked
    }

    @Test
    @DisplayName("Should retry GetTranslations 3 times if EnglishPhraseUniqueConstraintException")
    void shouldRetryGetTranslations3TimesIfEnglishPhraseUniqueConstraintException() {

        // GIVEN
        // NB: only one phrase in use
        Set<String> inputPhrases = Set.of(THE_QUICK_FOX_PHRASE);
        doReturn(Optional.empty()).when(dictionaryRepository).findByEnglishPhrase(THE_QUICK_FOX_PHRASE);

        // WHEN
        final Map<String, String> translations = dictionaryService.getTranslations(inputPhrases);

        // THEN
        assertThat(translations)
            .isNotNull()
            .containsOnlyKeys(THE_QUICK_FOX_PHRASE);

        // verify save called three times for only the one phrase: i.e. repeated three times
        verify(dictionaryRepository, times(3)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Should retry PutDictionary 3 times if EnglishPhraseUniqueConstraintException")
    void shouldRetryPutDictionary3TimesIfEnglishPhraseUniqueConstraintException() {

        // GIVEN
        given(securityUtils.hasRole(anyString())).willReturn(false);
        // NB: only one phrase in use
        final Dictionary dictionaryRequest = getDictionaryRequestWithoutTranslationPhrases(1);

        // WHEN
        dictionaryService.putDictionary(dictionaryRequest);

        // THEN
        // verify save called three times for only the one phrase: i.e. repeated three times
        verify(dictionaryRepository, times(3)).saveAndFlush(any());
    }

}
