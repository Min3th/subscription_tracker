package com.track.subscription_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/google",
            "/auth/refresh",
            "/auth/logout",
            "/notifications/unsubscribe",
            "/notifications/webhooks/sendgrid"
    );

    @Bean
    public OpenAPI subscriptionTrackerOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste an access token. Refresh tokens are not accepted by protected API endpoints.");

        return new OpenAPI()
                .info(new Info()
                        .title("Subtrak API")
                        .version("v1")
                        .description("API for subscription tracking, preferences, authentication, and reminder delivery.")
                        .contact(new Contact().name("Subtrak")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public OpenApiCustomizer publicEndpointSecurityCustomizer() {
        return openApi -> PUBLIC_PATHS.forEach(path -> {
            if (openApi.getPaths() == null || openApi.getPaths().get(path) == null) {
                return;
            }
            openApi.getPaths().get(path).readOperations()
                    .forEach(operation -> operation.setSecurity(List.of()));
        });
    }
}
