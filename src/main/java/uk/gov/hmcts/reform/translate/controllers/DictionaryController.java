package uk.gov.hmcts.reform.translate.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
import static uk.gov.hmcts.reform.translate.errorhandling.AuthError.AUTHENTICATION_TOKEN_INVALID;
import static uk.gov.hmcts.reform.translate.errorhandling.AuthError.UNAUTHORISED_S2S_SERVICE;
import static uk.gov.hmcts.reform.translate.model.ControllerConstants.DICTIONARY_URL;
import static uk.gov.hmcts.reform.translate.model.ControllerConstants.TRANSLATIONS_URL;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.MANAGE_TRANSLATIONS_ROLE;
import static uk.gov.hmcts.reform.translate.model.TranslationsRequest.BAD_REQUEST_MESSAGE_BAD_SCHEMA;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.translate.service.DictionaryService.INVALID_PAYLOAD_FORMAT;
import static uk.gov.hmcts.reform.translate.service.DictionaryService.BAD_REQUEST_MESSAGE_WELSH_NOT_ALLOWED;

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
            @ApiResponse(responseCode = "401", description = AUTHENTICATION_TOKEN_INVALID, content = @Content()),
            @ApiResponse(responseCode = "403", description = "One of the following reasons:\n"
                + "1. " + UNAUTHORISED_S2S_SERVICE + "\n"
                + "2. " + "User does not have 'manage-translations' role" + ".",
                content = @Content())
        })
    public Dictionary getDictionary() {
        return new Dictionary(dictionaryService.getDictionaryContents());
    }

    @PutMapping(path = DICTIONARY_URL, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a set of phrases for which translations may be provided.",
        description = "If user's current IDAM role is `manage-translations`\n\n"
            + "\t - User can submit English phrases with/without corresponding Welsh translations\n\n"
            + "If user's current IDAM role is `load translations`\n\n"
            + "\t - User can submit English phrases only\n\n"
            + "If calling service is on approved list of services for this endpoint that don't need to supply "
            + " Authorization header: then\n\n"
            + "\t - this endpoint can be called without supplying IDAM credentials (Authorization header) - Service"
            + " to Service authorization (ServiceAuthorization header) is still required.\n\n"
            + "\t - calling service can only submit English phrases\n\n",
        responses = {
            @ApiResponse(responseCode = "201", description = "Success"),
            @ApiResponse(responseCode = "400", description = "One of the following reasons:\n"
                + "1. " + INVALID_PAYLOAD_FORMAT + "\n"
                + "2. " + BAD_REQUEST_MESSAGE_BAD_SCHEMA + "\n"
                + "3. " + BAD_REQUEST_MESSAGE_WELSH_NOT_ALLOWED),
            @ApiResponse(responseCode = "401", description = AUTHENTICATION_TOKEN_INVALID, content = @Content()),
            @ApiResponse(responseCode = "403", description = "One of the following reasons:\n"
                + "1. " + UNAUTHORISED_S2S_SERVICE + "\n"
                + "2. The request should be from a valid service or the User does not have "
                + "'manage-translations,load-translations' role.",
                content = @Content())
        }
    )
    public void putDictionary(@RequestBody Dictionary dictionaryRequest,
                              @RequestHeader(SERVICE_AUTHORIZATION) @Parameter(hidden = true) String clientS2SToken) {
        dictionaryService.putDictionaryRoleCheck(clientS2SToken);
        dictionaryService.putDictionary(dictionaryRequest);
    }

    @PostMapping(path = TRANSLATIONS_URL, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get the Welsh translation of the provided set of English phrases.",
        description = "User does not require any specific roles to call this endpoint\n\n"
        + "This endpoint can be called without supplying IDAM credentials (Authorization header) - Service "
        + "to Service authorization (ServiceAuthorization header) is still required.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Translation returned successfully"),
            @ApiResponse(responseCode = "400", description = TranslationsRequest.BAD_REQUEST_MESSAGE_BAD_SCHEMA,
                content = @Content()),
            @ApiResponse(responseCode = "401", description = AUTHENTICATION_TOKEN_INVALID, content = @Content()),
            @ApiResponse(responseCode = "403", description = UNAUTHORISED_S2S_SERVICE, content = @Content())
        })
    public Dictionary getTranslation(@Valid @RequestBody final TranslationsRequest payload) {
        final Map<String, String> translations = dictionaryService.getTranslations(payload.getPhrases());
        return new Dictionary(translations);
    }
}
