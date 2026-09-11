package com.phabdev.starter.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Phabdev SaaS Starter API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
    @org.springframework.context.annotation.Bean
    org.springdoc.core.customizers.OpenApiCustomizer authMutationHeaders() {
        return api ->
                api.getPaths()
                        .forEach(
                                (path, item) -> {
                                    if (path.startsWith("/api/auth/"))
                                        item.readOperations()
                                                .forEach(
                                                        operation ->
                                                                operation.addParametersItem(
                                                                        new io.swagger.v3.oas.models
                                                                                        .parameters
                                                                                        .Parameter()
                                                                                .in("header")
                                                                                .name(
                                                                                        "X-Requested-With")
                                                                                .required(true)
                                                                                .schema(
                                                                                        new io
                                                                                                        .swagger
                                                                                                        .v3
                                                                                                        .oas
                                                                                                        .models
                                                                                                        .media
                                                                                                        .StringSchema()
                                                                                                ._default(
                                                                                                        "XMLHttpRequest"))));
                                });
    }
}
