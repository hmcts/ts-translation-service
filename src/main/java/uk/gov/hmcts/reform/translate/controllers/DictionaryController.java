package uk.gov.hmcts.reform.translate.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.model.TranslationsRequest;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import java.util.Map;
import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.translate.model.ControllerConstants.DICTIONARY_URL;
import static uk.gov.hmcts.reform.translate.model.ControllerConstants.TRANSLATIONS_URL;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.SERVICE_AUTHORIZATION;

@RestController
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @Autowired
    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping(path = DICTIONARY_URL, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Download a set of phrases potentially associated with a case type for which translations "
        + "will be eventually added.",
        description = "Users calling this endpoint must have the `" + MANAGE_TRANSLATIONS_ROLE + "` role",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dictionary returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        })
    public Dictionary getDictionary() {
        return new Dictionary(dictionaryService.getDictionaryContents());
    }

    @PutMapping(path = DICTIONARY_URL, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a set of phrases for which translations may be provided.",
        description = "When there is no extant entry for the specified English_phrase\n\n"
            + "\t- User's current role is \"" + MANAGE_TRANSLATIONS_ROLE + "\" - a new record is created\n\n"
            + "\t- User's current role is NOT \"" + MANAGE_TRANSLATIONS_ROLE + "\" - ignore any supplied Welsh "
            + "translations.\n\n"
            + "When there is an extant entry for the English phrase\n\n"
            + "\t- User's current role is \"" + MANAGE_TRANSLATIONS_ROLE + "\" - update the dictionary\n\n"
            + "\t- User's current role is  NOT \"" + MANAGE_TRANSLATIONS_ROLE
            + "\" ignore any supplied Welsh translations.\n\n",
        responses = {
            @ApiResponse(responseCode = "201", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Error occurred on the server")
        },
        parameters = {
            @Parameter(in = ParameterIn.HEADER,
            name = "ServiceAuthorization",
            description = "Service To Service (S2S) JWT",
            required = true,
            schema = @Schema(type = "string"))}
    )
    public void putDictionary(@RequestBody Dictionary dictionaryRequest,
                              @RequestHeader(SERVICE_AUTHORIZATION) String clientS2SToken) {
        dictionaryService.putDictionaryRoleCheck(clientS2SToken);
        dictionaryService.putDictionary(dictionaryRequest);
    }

    @PostMapping(path = TRANSLATIONS_URL, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get the Welsh translation of the provided set of English phrases.",
        description = "User does not require any specific roles to call this endpoint",
        responses = {
            @ApiResponse(responseCode = "200", description = "Translation returned successfully"),
            @ApiResponse(responseCode = "400", description = TranslationsRequest.BAD_REQUEST_MESSAGE),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        })
    public Dictionary getTranslation(@Valid @RequestBody final TranslationsRequest payload) {
        final Map<String, String> translations = dictionaryService.getTranslations(payload.getPhrases());
        return new Dictionary(translations);
    }
}
