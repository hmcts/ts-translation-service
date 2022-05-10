package uk.gov.hmcts.reform.translate.config;

//import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfiguration {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("Welsh Language Translation Service")
                      .description("Welsh Language Translation Service")
                      .version("v0.0.1"))
            .externalDocs(new ExternalDocumentation()
                              .description("README")
                              .url("https://github.com/hmcts/ts-translation-service"));
    }

}
