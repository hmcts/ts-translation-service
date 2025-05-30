package uk.gov.hmcts.reform.translate.helper;

public enum HttpMethodEnum {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    PATCH("PATCH"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    TRACE("TRACE");

    private final String value;

    HttpMethodEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
