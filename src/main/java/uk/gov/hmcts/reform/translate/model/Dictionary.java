package uk.gov.hmcts.reform.translate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
