package uk.gov.hmcts.reform.translate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.BAD_SCHEMA;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TranslationsRequest {

    @NotEmpty(message = BAD_SCHEMA)
    private Set<@NotBlank(message = BAD_SCHEMA) String> phrases;
}
