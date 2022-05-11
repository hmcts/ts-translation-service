package uk.gov.hmcts.reform.translate.controllers;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.translate.model.DictionaryRequest;
import uk.gov.hmcts.reform.translate.model.GetDictionaryResponse;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @Autowired
    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping(path = "/dictionary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dictionary returned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<GetDictionaryResponse> getDictionary() {
        GetDictionaryResponse dictionaryResponse = new GetDictionaryResponse(dictionaryService.getDictionaryContents());
        return new ResponseEntity<>(dictionaryResponse, HttpStatus.OK);
    }


    @PutMapping(path = "/dictionary", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Bad Request"),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Error occurred on the server")
    })
    public void putDictionary(@RequestBody DictionaryRequest dictionaryRequest) {
        dictionaryService.putDictionary(dictionaryRequest);
    }
}
