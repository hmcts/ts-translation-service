package uk.gov.hmcts.reform.translate.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.translate.BaseTest;
import uk.gov.hmcts.reform.translate.repository.DictionaryRepository;
import uk.gov.hmcts.reform.translate.repository.JpaDictionaryRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.translate.controllers.ControllerConstants.TESTING_SUPPORT_URL;
import static uk.gov.hmcts.reform.translate.controllers.ControllerConstants.TEST_PHRASES_URL;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.LOAD_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;

@SpringBootTest(properties = {"ts.endpoints.testing-support.enabled=true"})
public class TestingSupportControllerIT extends BaseTest {

    static final String DELETE_TEST_PHRASES_URL = TESTING_SUPPORT_URL + TEST_PHRASES_URL;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier(JpaDictionaryRepository.QUALIFIER)
    protected DictionaryRepository dictionaryRepository;

    @Nested
    @DisplayName("Delete Dictionary Test Phrases")
    class DeleteDictionaryTestPhrases {

        @Test
        @DisplayName("Delete called with no dictionary data to delete")
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT})
        void shouldReturn204WhenDictionaryHasNothingToDelete() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(delete(DELETE_TEST_PHRASES_URL).contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().is(HttpStatus.NO_CONTENT.value()))
                .andReturn();
        }

        @Test
        @DisplayName("Delete called with many dictionary translations to either delete or keep")
        @Sql(scripts = {DELETE_TRANSLATION_TABLES_SCRIPT, ADD_TEST_PHRASES_FOR_DELETION_SCRIPT})
        void shouldReturn204AndDeleteManyTestPhrasesFromDictionary() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // check data exists before delete
            assertTrue(dictionaryRepository.findByEnglishPhrase(DELETE_ME_PHRASE_WITH_TRANSLATION).isPresent());
            assertTrue(dictionaryRepository.findByEnglishPhrase(DELETE_ME_PHRASE_WITHOUT_TRANSLATION).isPresent());
            assertTrue(dictionaryRepository.findByEnglishPhrase(KEEP_ME_PHRASE_WITH_TRANSLATION).isPresent());
            assertTrue(dictionaryRepository.findByEnglishPhrase(KEEP_ME_PHRASE_WITHOUT_TRANSLATION).isPresent());

            // WHEN
            mockMvc.perform(delete(DELETE_TEST_PHRASES_URL).contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().is(HttpStatus.NO_CONTENT.value()))
                .andReturn();

            // THEN
            // verify deletes
            assertTrue(dictionaryRepository.findByEnglishPhrase(DELETE_ME_PHRASE_WITH_TRANSLATION).isEmpty());
            assertTrue(dictionaryRepository.findByEnglishPhrase(DELETE_ME_PHRASE_WITHOUT_TRANSLATION).isEmpty());
            // verify keeps
            assertTrue(dictionaryRepository.findByEnglishPhrase(KEEP_ME_PHRASE_WITH_TRANSLATION).isPresent());
            assertTrue(dictionaryRepository.findByEnglishPhrase(KEEP_ME_PHRASE_WITHOUT_TRANSLATION).isPresent());

        }

        @Test
        @DisplayName("Delete called but not permitted without `" + MANAGE_TRANSLATIONS_ROLE + "` role")
        void shouldReturn403WhenUserDoesNotHaveManageTranslationsRole() throws Exception {

            // GIVEN
            stubUserInfo(LOAD_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvc.perform(delete(DELETE_TEST_PHRASES_URL).contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()))
                .andReturn();
        }
    }

    @Nested
    @DisplayName("Test Support Endpoint Disabled")
    @SpringBootTest(properties = {"ts.endpoints.testing-support.enabled=false"})
    class TestingSupportEndpointDisabled {

        @Autowired // NB: using a fresh instance, so it loads using new test properties
        private MockMvc mockMvcWithEndpointDisabled;

        @Test
        @DisplayName("Delete Dictionary Test Phrases: should return NOT_FOUND if endpoint is disabled")
        void shouldReturnNotFoundForDeleteDictionaryIfTestSupportEndpointDisabled() throws Exception {

            // GIVEN
            stubUserInfo(MANAGE_TRANSLATIONS_ROLE);

            // WHEN / THEN
            mockMvcWithEndpointDisabled.perform(delete(DELETE_TEST_PHRASES_URL).contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andReturn();
        }

    }

}
