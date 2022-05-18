package uk.gov.hmcts.reform.translate.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@AllArgsConstructor
@Data
public class Dictionary {
    private Map<String, String> translations;
}
