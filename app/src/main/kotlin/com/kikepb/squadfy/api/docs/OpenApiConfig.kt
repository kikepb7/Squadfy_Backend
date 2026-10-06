package com.kikepb.squadfy.api.docs

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI contract of /api/v1 (spec 007). Served only when springdoc is enabled (dev profile):
 * Swagger UI at /swagger-ui.html, JSON at /v3/api-docs.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun squadfyOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Squadfy API")
                    .version("v1")
                    .description(
                        """
                        Clubs, weekly matches with call-ups and waitlist, balanced team draws, ratings, chat and devices.

                        Errors: `{ "code": "...", "message": "..." }` (validation: `{ "code": "VALIDATION_ERROR", "errors": [...] }`).
                        400 invalid request or business rule · 401 missing/invalid token · 403 not allowed · 404 not found · 409 conflict.
                        """.trimIndent()
                    )
            )
            .components(
                Components().addSecuritySchemes(
                    BEARER_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token from POST /api/v1/auth/login")
                )
            )
            .addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

    private companion object {
        const val BEARER_SCHEME = "bearerAuth"
    }
}
