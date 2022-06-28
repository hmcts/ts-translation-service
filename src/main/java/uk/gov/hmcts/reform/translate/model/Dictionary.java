package uk.gov.hmcts.reform.translate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Dictionary {

    @Schema(description = "A map of phrases and corresponding (possibly not yet provided) translations",
        example = "{"
                    + "\"English phrase 1\": \"Welsh translation 1\","
                    + "\"English phrase 2\": \"\","
                    + "\"English phrase 3\": \"Welsh translation 3\""
                + "}"
    )
    Map<String, String> translations;
}
