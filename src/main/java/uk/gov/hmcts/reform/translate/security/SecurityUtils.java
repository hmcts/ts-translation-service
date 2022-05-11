package uk.gov.hmcts.reform.translate.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class SecurityUtils {

    public UserInfo getUserInfo() {
        UserInfo userInfo = UserInfo.builder()
            .familyName("NE_NU_NE")
            .name("PEPE")
            .givenName("givenName")
            .uid("11111111")
            .roles(Arrays.asList("ROLE", "manage-translations"))
            .sub("sub")
            .build();
        return userInfo;
    }

    public boolean hasManageTranslationsRole(List<String> roles) {
        String manageTranslationsRole = "manage-translations";
        return roles.contains(manageTranslationsRole);
    }
}
