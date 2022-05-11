package uk.gov.hmcts.reform.translate.service;

import groovy.lang.IntRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.translate.data.DictionaryEntity;
import uk.gov.hmcts.reform.translate.helper.DictionaryMapper;
import uk.gov.hmcts.reform.translate.model.DictionaryRequest;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.security.SecurityUtils;
import uk.gov.hmcts.reform.translate.security.UserInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.JUnitAssertionsShouldIncludeMessage"})
class DictionaryServiceTest {

    @Mock
    DictionaryRepository dictionaryRepository;

    @Mock
    DictionaryMapper dictionaryMapper;

    @Mock
    SecurityUtils securityUtils;


    @Mock
    Iterable<DictionaryEntity> repositoryResults;

    @InjectMocks
    DictionaryService dictionaryService;

    @Test
    void shouldReturnDictionaryContents() {
        Map<String, String> expectedMapKeysAndValues = new HashMap<>();

        new IntRange(1, 3).forEach(i -> expectedMapKeysAndValues.put("english" + i, "translated" + i));

        var dictionaryEntities = expectedMapKeysAndValues.entrySet().stream()
            .map(es -> createDictionaryEntity(
                es.getKey(),
                es.getValue()
            ))
            .toArray(DictionaryEntity[]::new);

        var spliterator = Arrays.spliterator(dictionaryEntities);
        given(repositoryResults.spliterator()).willReturn(spliterator);
        given(dictionaryRepository.findAll()).willReturn(repositoryResults);

        assertTrue(dictionaryService.getDictionaryContents().entrySet()
                       .containsAll(expectedMapKeysAndValues.entrySet()));
    }

    private DictionaryEntity createDictionaryEntity(String phrase, String translationPhrase) {
        final var dictionaryEntity = new DictionaryEntity();
        dictionaryEntity.setEnglishPhrase(phrase);
        dictionaryEntity.setEnglishPhrase(translationPhrase);
        return dictionaryEntity;
    }

    @Test
    void shouldReturnEmptyDictionaryContents() {
        given(dictionaryRepository.findAll()).willReturn(repositoryResults);
        assertTrue(dictionaryService.getDictionaryContents().isEmpty());
    }


    @Test
    void shouldPutANewDictionaryForUserWithManageTranslationsRole() {
        final DictionaryRequest dictionaryRequest = getDictionaryRequest(1, 3);
        given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
        given(securityUtils.hasManageTranslationsRole(anyList())).willReturn(true);
        dictionaryService.putDictionary(dictionaryRequest);

        verify(dictionaryRepository, times(3)).findByEnglishPhrase(any());
        verify(securityUtils, times(3)).hasManageTranslationsRole(any());
        verify(dictionaryMapper, times(3)).modelToEntityWithTranslationUploadEntity(any(), any());
        verify(dictionaryRepository, times(3)).save(any());
    }

    @Test
    void shouldPutANewDictionaryForUserWithoutManageTranslationsRole() {
        final DictionaryRequest dictionaryRequest = getDictionaryRequest(1, 3);
        given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
        given(securityUtils.hasManageTranslationsRole(anyList())).willReturn(false);
        dictionaryService.putDictionary(dictionaryRequest);

        verify(dictionaryRepository, times(3)).findByEnglishPhrase(any());
        verify(securityUtils, times(3)).hasManageTranslationsRole(any());
        verify(dictionaryMapper, times(3)).modelToEntityWithoutTranslationPhrase(any());
        verify(dictionaryRepository, times(3)).save(any());
    }

    @Test
    void shouldUpdateADictionaryForUserWithManageTranslationsRole() {
        final DictionaryRequest dictionaryRequest = getDictionaryRequest(1, 1);
        final DictionaryEntity dictionaryEntity =
            createDictionaryEntity("english_1", "translated_1");

        given(dictionaryRepository.findByEnglishPhrase(any())).willReturn(Optional.of(dictionaryEntity));

        given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
        given(securityUtils.hasManageTranslationsRole(anyList())).willReturn(true);
        dictionaryService.putDictionary(dictionaryRequest);

        verify(dictionaryRepository, times(1)).findByEnglishPhrase(any());
        verify(securityUtils, times(1)).hasManageTranslationsRole(any());
        verify(dictionaryRepository, times(1)).save(any());
    }


    @Test
    void shouldUpdateADictionaryForUserWithoutManageTranslationsRole() {
        final DictionaryRequest dictionaryRequest = getDictionaryRequest(1, 1);
        final DictionaryEntity dictionaryEntity =
            createDictionaryEntity("english_1", "translated_1");

        given(dictionaryRepository.findByEnglishPhrase(any())).willReturn(Optional.of(dictionaryEntity));

        given(securityUtils.getUserInfo()).willReturn(getUserInfoWithManageTranslationsRole());
        given(securityUtils.hasManageTranslationsRole(anyList())).willReturn(false);
        dictionaryService.putDictionary(dictionaryRequest);

        verify(dictionaryRepository, times(1)).findByEnglishPhrase(any());
        verify(securityUtils, times(1)).hasManageTranslationsRole(any());
        verify(dictionaryRepository, times(0)).save(any());
    }


    private DictionaryRequest getDictionaryRequest(int from, int to) {
        final Map<String, String> expectedMapKeysAndValues = new HashMap<>();
        new IntRange(from, to).forEach(i -> expectedMapKeysAndValues.put("english_" + i, "translated_" + i));
        return new DictionaryRequest(expectedMapKeysAndValues);
    }


    private UserInfo getUserInfoWithManageTranslationsRole() {
        UserInfo userInfo = UserInfo.builder()
            .familyName("NE_NU_NE")
            .name("PEPE")
            .givenName("givenName")
            .uid("11111111")
            .roles(Arrays.asList("ROLE", "manage-translations"))
            .sub("sub")
            .build();
        return userInfo;
    }
}

