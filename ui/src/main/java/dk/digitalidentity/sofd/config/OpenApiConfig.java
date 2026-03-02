package dk.digitalidentity.sofd.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi classificationOpenApi() {
        return GroupedOpenApi.builder()
                .group("classification")
                .packagesToScan("dk.digitalidentity.sofd.controller.api.classification")
                .build();
    }

    @Bean
    public GroupedOpenApi orgUnitOpenApi() {
        return GroupedOpenApi.builder()
                .group("orgUnit")
                .pathsToMatch("/api/orgUnit/**")
                .build();
    }

    @Bean
    public GroupedOpenApi personOpenApi() {
        return GroupedOpenApi.builder()
                .group("person")
                .packagesToScan("dk.digitalidentity.sofd.controller.api.person")
                .build();
    }

    @Bean
    public OpenAPI OS2sofdOpenAPI() {
        final String securitySchemeName = "ApiKey";

        return new OpenAPI()
                .info(new Info()
                        .title("OS2sofd API")
                        .description("API for writing to OS2sofd")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("ApiKey")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("API Key authentication")));
    }
}