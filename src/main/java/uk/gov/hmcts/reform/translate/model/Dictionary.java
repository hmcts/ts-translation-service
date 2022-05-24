package uk.gov.hmcts.reform.translate.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;

@Getter
public class Dictionary {

    private final Map<String, String> translations;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Dictionary(@JsonProperty("translations") Map<String, String> translations) {
        this.translations = translations;
    }
}
