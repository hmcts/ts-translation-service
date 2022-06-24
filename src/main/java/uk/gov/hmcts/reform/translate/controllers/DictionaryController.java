package uk.gov.hmcts.reform.translate.controllers;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.BAD_SCHEMA;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.WELSH_NOT_ALLOWED;
import static uk.gov.hmcts.reform.translate.model.ControllerConstants.DICTIONARY_URL;
import static uk.gov.hmcts.reform.translate.model.ControllerConstants.TRANSLATIONS_URL;
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dictionary returned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Dictionary getDictionary() {
        return new Dictionary(dictionaryService.getDictionaryContents());
    }

    @PutMapping(path = DICTIONARY_URL, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Success"),
        @ApiResponse(responseCode = "400", description = "One or more of the following reasons:"
            + "\n1) " + BAD_SCHEMA
            + "\n2) " + WELSH_NOT_ALLOWED),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Error occurred on the server")
    })
    public void putDictionary(@RequestBody Dictionary dictionaryRequest,
                              @RequestHeader(SERVICE_AUTHORIZATION) String clientS2SToken) {

        dictionaryService.putDictionaryRoleCheck(clientS2SToken);
        dictionaryService.putDictionary(dictionaryRequest);
    }

    @PostMapping(path = TRANSLATIONS_URL, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Translation returned successfully"),
        @ApiResponse(responseCode = "400", description = BAD_SCHEMA),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Dictionary getTranslation(@Valid @RequestBody final TranslationsRequest payload) {
        final Map<String, String> translations = dictionaryService.getTranslations(payload.getPhrases());
        return new Dictionary(translations);
    }
}
