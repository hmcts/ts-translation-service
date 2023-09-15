package uk.gov.hmcts.reform.translate.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
@JsonInclude(Include.NON_NULL)
public class Translation {

    @NonNull
    private String translation;
    private Boolean yesOrNo;
    private String yes;
    private String no;

    public boolean isYesOrNo() {
        return yesOrNo == null ? false : yesOrNo.booleanValue();
    }

}
