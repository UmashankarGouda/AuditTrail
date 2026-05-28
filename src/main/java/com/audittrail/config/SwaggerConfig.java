package com.audittrail.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        
        openAPI.info(new Info()
                .title("DeployGuard API")
                .version("0.1.0-BETA")
                .description("Deployment Pipeline Audit & Release Management System"));
        
        openAPI.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
        
        openAPI.components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token - paste your token from /api/auth/login")));
        
        return openAPI;
    }
}
