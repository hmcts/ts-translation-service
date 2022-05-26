package uk.gov.hmcts.reform.translate.model;

import lombok.Value;

import java.util.Map;

@Value
public class Dictionary {
    Map<String, String> translations;
}
