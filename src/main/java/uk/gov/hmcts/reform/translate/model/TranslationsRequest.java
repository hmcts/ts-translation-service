package uk.gov.hmcts.reform.translate.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.BAD_SCHEMA;

@Data
@NoArgsConstructor
public class TranslationsRequest {

    @Schema(description = "A set of phrases for which translations may be provided.",
        example = "[\"English Phrase 1\", \"English Phrase 2\", \"English Phrase 3\"]")
    @NotEmpty(message = BAD_SCHEMA)
    private Set<@NotBlank(message = BAD_SCHEMA) String> phrases;

    @JsonCreator
    public TranslationsRequest(Set<String> phrases) {
        this.phrases = phrases;
    }
}
