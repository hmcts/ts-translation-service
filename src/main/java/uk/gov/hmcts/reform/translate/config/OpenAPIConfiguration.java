package uk.gov.hmcts.reform.translate.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import static uk.gov.hmcts.reform.translate.security.SecurityUtils.AUTHORIZATION;
import static uk.gov.hmcts.reform.translate.security.SecurityUtils.SERVICE_AUTHORIZATION;

@Configuration
public class OpenAPIConfiguration {

    private static final String DESCRIPTION = "Provides a capability for service users to maintain Welsh translations."
        + "  This enables Welsh Language Support for Professional Users; allowing EXUI users to choose to view the UI"
        + " in Welsh";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .components(new Components()
                            .addSecuritySchemes(
                                AUTHORIZATION,
                                new SecurityScheme()
                                                    .name(AUTHORIZATION)
                                                    .type(SecurityScheme.Type.HTTP)
                                                    .scheme("bearer")
                                                    .bearerFormat("JWT")
                                                    .description("Valid IDAM user token, (Bearer keyword is "
                                                                     + "added automatically)")
                            )
                            .addSecuritySchemes(SERVICE_AUTHORIZATION,
                                                new SecurityScheme()
                                                    .in(SecurityScheme.In.HEADER)
                                                    .name(SERVICE_AUTHORIZATION)
                                                    .type(SecurityScheme.Type.APIKEY)
                                                    .scheme("bearer")
                                                    .bearerFormat("JWT")
                                                    .description("Valid Service-to-Service JWT token for a "
                                                                     + "whitelisted micro-service")
                            )
            )
            .info(new Info().title("Welsh Language Translation Service")
                      .description(DESCRIPTION)
                      .version("v0.0.1"))
            .externalDocs(new ExternalDocumentation()
                              .description("README")
                              .url("https://github.com/hmcts/ts-translation-service#readme"))
            .addSecurityItem(new SecurityRequirement().addList(AUTHORIZATION))
            .addSecurityItem(new SecurityRequirement().addList(SERVICE_AUTHORIZATION));
    }
}
