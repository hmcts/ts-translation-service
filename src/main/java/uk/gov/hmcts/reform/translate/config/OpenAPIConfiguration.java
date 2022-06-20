package uk.gov.hmcts.reform.translate.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfiguration {

    private static final String DESCRIPTION = "Provides a capability for service users to maintain Welsh translations."
        + "  This enables Welsh Language Support for Professional Users; allowing EXUI users to choose to view the UI"
        + " in Welsh";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .components(new Components()
                            .addSecuritySchemes("bearerAuth",
                                                new SecurityScheme()
                                                    .name("bearerAuth")
                                                    .type(SecurityScheme.Type.HTTP)
                                                    .scheme("bearer")
                                                    .bearerFormat("JWT"))
                            .addSecuritySchemes("serviceAuthorization",
                                                new SecurityScheme()
                                                    .in(SecurityScheme.In.HEADER)
                                                    .name("ServiceAuthorization")
                                                    .type(SecurityScheme.Type.APIKEY)
                                                    .scheme("bearer")
                                                    .bearerFormat("JWT")
                            )
            )
            .info(new Info().title("Welsh Language Translation Service")
                      .description(DESCRIPTION)
                      .version("v0.0.1"))
            .externalDocs(new ExternalDocumentation()
                              .description("README")
                              .url("https://github.com/hmcts/ts-translation-service#readme"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .addSecurityItem(new SecurityRequirement().addList("serviceAuthorization"));
    }
}
