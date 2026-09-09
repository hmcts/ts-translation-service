package uk.gov.hmcts.reform.translate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
public class Translation {

    @NonNull
    @JsonProperty("translation")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String translatedText;
    private Boolean yesOrNo;
    private String yes;
    private String no;

    public String getTranslation() {
        return translatedText;
    }

    public void setTranslation(String translation) {
        this.translatedText = translation;
    }

    public boolean isYesOrNo() {
        return Boolean.TRUE.equals(yesOrNo);
    }

}
