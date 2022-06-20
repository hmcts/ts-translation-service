package uk.gov.hmcts.reform.translate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TranslationsRequest {
    public static final String BAD_REQUEST_MESSAGE = "Bad Request (001 bad schema)";

    @Schema(description = "A set of phrases for which translations may be provided.",
        example = "[\"English Phrase 1\", \"English Phrase 2\", \"English Phrase 3\"]")
    @NotEmpty(message = BAD_REQUEST_MESSAGE)
    private Set<@NotBlank(message = BAD_REQUEST_MESSAGE) String> phrases;
}
