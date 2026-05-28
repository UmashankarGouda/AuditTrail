package com.audittrail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "AuditTrail API",
        version = "0.1.0-BETA",
        description = "Deployment Audit Trail & Change Management System"
    ),
    security = @SecurityRequirement(name = "Bearer Authentication")
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT auth token. Enter 'Bearer <token>' in the format"
)
public class AuditTrailApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditTrailApplication.class, args);
    }
}
