package uk.gov.hmcts.reform.translate.wiremock.extensions;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpHeaders;


/*
 * Replaces response body with the OAuth JWK Set, i.e. the public keys used to sign the mock OAAuth token
 */
@Slf4j
public class CustomisedResponseTransformer implements ResponseDefinitionTransformerV2 {

    @Override
    public String getName() {
        return "keep-alive-disabler";
    }

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        return ResponseDefinitionBuilder.like(serveEvent.getResponseDefinition())
            .withHeader(HttpHeaders.CONNECTION, "close")
            .build();
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
}
