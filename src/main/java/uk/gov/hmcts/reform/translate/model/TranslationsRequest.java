package uk.gov.hmcts.reform.translate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.BAD_SCHEMA;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationsRequest {

    @Schema(description = "A set of phrases for which translations may be provided.",
        example = "[\"English Phrase 1\", \"English Phrase 2\", \"English Phrase 3\"]")
    @NotEmpty(message = BAD_SCHEMA)
    private Set<@NotBlank(message = BAD_SCHEMA) String> phrases;
}
