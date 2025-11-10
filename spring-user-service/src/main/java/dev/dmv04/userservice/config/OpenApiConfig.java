package dev.dmv04.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPIV3Parser()
                .readLocation(
                        "/openapi.yml",
                        Collections.emptyList(),
                        new ParseOptions()
                )
                .getOpenAPI();
    }
}
