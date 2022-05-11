package uk.gov.hmcts.reform.translate.model;

import lombok.Value;

import java.util.Map;

@Value
public class TranslationsResponse {
    Map<String, String> translations;
}
