package uk.gov.hmcts.reform.translate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TranslationsRequest {
    private static final String BAD_REQUEST_MESSAGE = "Bad Request (001 bad schema)";

    @NotEmpty(message = BAD_REQUEST_MESSAGE)
    private List<@NotBlank(message = BAD_REQUEST_MESSAGE) String> phrases;
}
