package uk.gov.hmcts.reform.translate.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
public class Translation {

    @NonNull
    private String translation;
    private Boolean yesOrNo;
    private String yes;
    private String no;

    @JsonCreator
    public Translation(@JsonProperty("translation") @NonNull String translation,
                       @JsonProperty("yesOrNo") Boolean yesOrNo,
                       @JsonProperty("yes") String yes,
                       @JsonProperty("no") String no) {
        this.translation = translation;
        this.yesOrNo = yesOrNo;
        this.yes = yes;
        this.no = no;
    }

    public boolean isYesOrNo() {
        return yesOrNo == null ? false : yesOrNo.booleanValue();
    }

}
