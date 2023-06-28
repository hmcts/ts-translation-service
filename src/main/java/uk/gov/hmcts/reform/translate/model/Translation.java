package uk.gov.hmcts.reform.translate.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
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
