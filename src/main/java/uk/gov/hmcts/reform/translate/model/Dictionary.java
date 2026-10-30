package uk.gov.hmcts.reform.translate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

@Data
@NoArgsConstructor
public class Dictionary {

    @Schema(description = "A map of phrases and corresponding translation object "
            + "(with possibly not yet provided translation)",
        example = "{"
                    + "\"English phrase 1\": {\"translation\":\"\"},"
                    + "\"English phrase 2\": {\"translation\":\"Welsh translation 1\"},"
                    + "\"English phrase 3\": {"
                        + "\"translation\": \"Welsh translation 1\", "
                        + "\"yesOrNo\":  true,"
                        + "\"yes\": \"Welsh Yes Translation\","
                        + "\"no\": \"Welsh No Translation\","
                    + "}"
                + "}"
    )
    Map<String, Translation> translations;

    @JsonCreator
    public Dictionary(Map<String, Translation> translations) {
        this.translations = translations;
    }
}
