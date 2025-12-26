package com.giggles.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Giggles Authentication API")
                        .version("1.0.0")
                        .description("JWT-based authentication system with User and Admin roles")
                        .contact(new Contact()
                                .name("Giggles Team")
                                .email("support@giggles.com")));
    }
}

