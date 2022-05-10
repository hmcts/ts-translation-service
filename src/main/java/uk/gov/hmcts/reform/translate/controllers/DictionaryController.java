package uk.gov.hmcts.reform.translate.controllers;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.translate.model.Dictionary;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

@RestController
@RequestMapping("/dictionary")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @Autowired
    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dictionary returned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Dictionary getDictionary() {
        return new Dictionary(dictionaryService.getDictionaryContents());
    }
}