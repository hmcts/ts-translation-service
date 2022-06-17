package uk.gov.hmcts.reform.translate.model;

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
    public static final String BAD_REQUEST_MESSAGE_BAD_SCHEMA = "Bad Request (001 bad schema)";

    @NotEmpty(message = BAD_REQUEST_MESSAGE_BAD_SCHEMA)
    private Set<@NotBlank(message = BAD_REQUEST_MESSAGE_BAD_SCHEMA) String> phrases;
}
