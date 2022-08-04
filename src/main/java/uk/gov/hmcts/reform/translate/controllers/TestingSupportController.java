package uk.gov.hmcts.reform.translate.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import static uk.gov.hmcts.reform.translate.controllers.ControllerConstants.TESTING_SUPPORT_URL;
import static uk.gov.hmcts.reform.translate.controllers.ControllerConstants.TEST_PHRASES_URL;
import static uk.gov.hmcts.reform.translate.errorhandling.AuthError.AUTHENTICATION_TOKEN_INVALID;
import static uk.gov.hmcts.reform.translate.errorhandling.AuthError.UNAUTHORISED_S2S_SERVICE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;

@RestController
@RequestMapping(path = TESTING_SUPPORT_URL)
@ConditionalOnProperty(value = "ts.endpoints.testing-support.enabled", havingValue = "true")
public class TestingSupportController {

    private final DictionaryService dictionaryService;

    @Autowired
    public TestingSupportController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }


    @DeleteMapping(path = TEST_PHRASES_URL)
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletes test phrases from dictionary added during automated testing.",
        description = "Users calling this endpoint must have the `" + MANAGE_TRANSLATIONS_ROLE + "` role",
        responses = {
            @ApiResponse(responseCode = "204", description = "Test data deleted successfully"),
            @ApiResponse(responseCode = "401", description = AUTHENTICATION_TOKEN_INVALID, content = @Content()),
            @ApiResponse(responseCode = "403", description = "One of the following reasons:\n"
                + "1. " + UNAUTHORISED_S2S_SERVICE + "\n"
                + "2. " + "User does not have '" + MANAGE_TRANSLATIONS_ROLE + "' role.",
                content = @Content())
        })
    public void deleteDictionaryTestPhrases() {
        dictionaryService.deleteTestPhrases();
    }

}
