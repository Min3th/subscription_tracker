package com.track.subscription_service.integration;

import com.track.subscription_service.notification.service.SendGridEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "google.client.id=test-google-client-id",
        "app.frontend.origins=https://frontend.example",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.sendgrid.eventWebhookPublicKey="
})
class ApiBoundaryIntegrationTest extends PostgresIntegrationTest {

    private static final String ALLOWED_ORIGIN = "https://frontend.example";
    private static final String SIGNATURE_HEADER = "X-Twilio-Email-Event-Webhook-Signature";
    private static final String TIMESTAMP_HEADER = "X-Twilio-Email-Event-Webhook-Timestamp";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SendGridEventService eventService;

    @Test
    void openApiDocumentIsPublicAndDocumentsBearerAccessTokens() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Subtrak API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$['paths']['/auth/refresh']['post']['security']").isEmpty())
                .andExpect(jsonPath("$['paths']['/notifications/webhooks/sendgrid']['post']['security']")
                        .isEmpty());
    }

    @Test
    void swaggerUiIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/swagger-ui/")));
    }

    @Test
    void allowedCredentialedCorsPreflightReturnsConfiguredHeaders() throws Exception {
        mockMvc.perform(options("/subscriptions")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        containsString("GET")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("authorization")));
    }

    @Test
    void disallowedCorsPreflightIsRejectedWithoutAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/subscriptions")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void allowedOriginIsPreservedOnProtectedApiAuthenticationErrors() throws Exception {
        mockMvc.perform(get("/subscriptions")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void verifiedSendGridWebhookIsAcceptedWithoutBearerAuthentication() throws Exception {
        String payload = "[{\"email\":\"bounce@example.com\",\"event\":\"bounce\"}]";

        mockMvc.perform(post("/notifications/webhooks/sendgrid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(SIGNATURE_HEADER, "valid-signature")
                        .header(TIMESTAMP_HEADER, "1784890000")
                        .content(payload))
                .andExpect(status().isNoContent());

        verify(eventService).process(payload, "valid-signature", "1784890000");
    }

    @Test
    void invalidWebhookSignatureReturnsStableUnauthorizedError() throws Exception {
        String payload = "[]";
        doThrow(new SecurityException("invalid"))
                .when(eventService).process(payload, null, null);

        mockMvc.perform(post("/notifications/webhooks/sendgrid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature"))
                .andExpect(jsonPath("$.path").value("/notifications/webhooks/sendgrid"));
    }

    @Test
    void malformedWebhookPayloadReturnsStableBadRequestError() throws Exception {
        String payload = "{}";
        doThrow(new IllegalArgumentException("invalid"))
                .when(eventService).process(payload, "signature", "timestamp");

        mockMvc.perform(post("/notifications/webhooks/sendgrid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(SIGNATURE_HEADER, "signature")
                        .header(TIMESTAMP_HEADER, "timestamp")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"))
                .andExpect(jsonPath("$.message").value("Invalid webhook payload"))
                .andExpect(jsonPath("$.path").value("/notifications/webhooks/sendgrid"));
    }
}
