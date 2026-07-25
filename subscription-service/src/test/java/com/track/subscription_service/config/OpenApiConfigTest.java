package com.track.subscription_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void documentsAccessTokenAuthentication() {
        OpenAPI openApi = config.subscriptionTrackerOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Subtrak API");
        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfig.BEARER_SCHEME);
        assertThat(openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_SCHEME).getScheme())
                .isEqualTo("bearer");
        assertThat(openApi.getSecurity().get(0))
                .containsKey(OpenApiConfig.BEARER_SCHEME);
    }

    @Test
    void removesBearerRequirementFromPublicOperations() {
        Operation publicOperation = new Operation()
                .addSecurityItem(new SecurityRequirement().addList(OpenApiConfig.BEARER_SCHEME));
        Operation protectedOperation = new Operation()
                .addSecurityItem(new SecurityRequirement().addList(OpenApiConfig.BEARER_SCHEME));
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/auth/refresh", new PathItem().post(publicOperation))
                .addPathItem("/subscriptions", new PathItem().get(protectedOperation)));

        config.publicEndpointSecurityCustomizer().customise(openApi);

        assertThat(publicOperation.getSecurity()).isEmpty();
        assertThat(protectedOperation.getSecurity()).hasSize(1);
    }
}
