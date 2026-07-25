package com.track.subscription_service.integration;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "google.client.id=test-google-client-id",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.sendgrid.eventWebhookPublicKey="
})
class SubscriptionTenantIsolationApiIntegrationTest extends PostgresIntegrationTest {

    private static final String USER_PREFIX = "subscription-api-tenant-";
    private static final String UPDATE_BODY = """
            {
              "name": "Attempted overwrite",
              "cost": 99.0000,
              "currency": "USD",
              "type": "RECURRING",
              "category": "SOFTWARE",
              "startDate": "2026-01-01",
              "billingIntervalUnit": "MONTH",
              "billingIntervalCount": 1,
              "emailNotificationsEnabled": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private long firstUserId;
    private long firstSubscriptionId;
    private long secondSubscriptionId;
    private String firstUserToken;

    @BeforeEach
    void createTwoTenants() {
        firstUserId = createUser("first");
        long secondUserId = createUser("second");
        firstSubscriptionId = createSubscription(firstUserId, "First tenant subscription");
        secondSubscriptionId = createSubscription(secondUserId, "Second tenant subscription");
        firstUserToken = accessToken(USER_PREFIX + "first");
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id LIKE ?", USER_PREFIX + "%");
    }

    @Test
    void listAndGetReturnOnlyTheAuthenticatedTenantsSubscriptions() throws Exception {
        mockMvc.perform(get("/subscriptions")
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(firstSubscriptionId))
                .andExpect(jsonPath("$[0].name").value("First tenant subscription"));

        mockMvc.perform(get("/subscriptions/{id}", firstSubscriptionId)
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstSubscriptionId));
    }

    @Test
    void anotherTenantsSubscriptionCannotBeReadById() throws Exception {
        mockMvc.perform(get("/subscriptions/{id}", secondSubscriptionId)
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void anotherTenantsSubscriptionCannotBeUpdated() throws Exception {
        mockMvc.perform(put("/subscriptions/{id}", secondSubscriptionId)
                        .header("Authorization", bearer(firstUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/subscriptions/{id}", secondSubscriptionId)
                        .header("Authorization", bearer(accessToken(USER_PREFIX + "second"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Second tenant subscription"));
    }

    @Test
    void anotherTenantsSubscriptionCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/subscriptions/{id}", secondSubscriptionId)
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/subscriptions/{id}", secondSubscriptionId)
                        .header("Authorization", bearer(accessToken(USER_PREFIX + "second"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondSubscriptionId));
    }

    @Test
    void createdSubscriptionIsOwnedByTheAuthenticatedTenant() throws Exception {
        mockMvc.perform(post("/subscriptions")
                        .header("Authorization", bearer(firstUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY.replace("Attempted overwrite", "Authenticated owner")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/subscriptions/\\d+")))
                .andExpect(jsonPath("$.name").value("Authenticated owner"));

        org.junit.jupiter.api.Assertions.assertEquals(firstUserId, jdbc.queryForObject(
                "SELECT user_id FROM subscription WHERE name = 'Authenticated owner'",
                Long.class));
    }

    private long createUser(String suffix) {
        String googleId = USER_PREFIX + suffix;
        return jdbc.queryForObject("""
                INSERT INTO users (google_id, email, name)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, googleId, suffix + "@example.com", "Tenant " + suffix);
    }

    private long createSubscription(long userId, String name) {
        return jdbc.queryForObject("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    billing_interval_unit, billing_interval_count,
                    email_notifications_enabled, user_id
                ) VALUES (?, 10.0000, 'USD', 'RECURRING', 'SOFTWARE', '2026-01-01',
                    'MONTH', 1, TRUE, ?)
                RETURNING id
                """, Long.class, name, userId);
    }

    private String accessToken(String googleId) {
        User user = new User();
        user.setGoogleId(googleId);
        return jwtService.generateAccessToken(user);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
