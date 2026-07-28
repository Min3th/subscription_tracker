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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SubscriptionSuggestionApiIntegrationTest extends PostgresIntegrationTest {
    private static final String USER_PREFIX = "suggestion-api-";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private long firstUserId;
    private long secondUserId;
    private String firstToken;
    private String secondToken;

    @BeforeEach
    void createUsers() {
        firstUserId = createUser("first");
        secondUserId = createUser("second");
        firstToken = accessToken(USER_PREFIX + "first");
        secondToken = accessToken(USER_PREFIX + "second");
    }

    @AfterEach
    void removeUsers() {
        jdbc.update("DELETE FROM users WHERE google_id LIKE ?", USER_PREFIX + "%");
    }

    @Test
    void listsAndConfirmsOnlyTheAuthenticatedUsersPendingSuggestion() throws Exception {
        UUID suggestionId = createSuggestion(firstUserId, "Netflix", "EUR", "12.9900");
        createSuggestion(secondUserId, "Spotify", "USD", "10.9900");

        mockMvc.perform(get("/inbound-email/suggestions")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(suggestionId.toString()))
                .andExpect(jsonPath("$[0].provider").value("Netflix"))
                .andExpect(jsonPath("$[0].currency").value("EUR"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(post("/inbound-email/suggestions/{id}/confirm", suggestionId)
                        .header("Authorization", bearer(secondToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/inbound-email/suggestions/{id}/confirm", suggestionId)
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Netflix Premium"))
                .andExpect(jsonPath("$.currency").value("EUR"));

        Long subscriptionId = jdbc.queryForObject("""
                SELECT confirmed_subscription_id
                FROM subscription_suggestion
                WHERE id = ?
                """, Long.class, suggestionId);
        assertNotNull(subscriptionId);
        assertEquals("CONFIRMED", suggestionStatus(suggestionId));
        assertEquals("EUR", jdbc.queryForObject(
                "SELECT currency FROM subscription WHERE id = ?",
                String.class, subscriptionId));

        mockMvc.perform(post("/inbound-email/suggestions/{id}/confirm", suggestionId)
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    void ignoringAPendingSuggestionCreatesNoSubscription() throws Exception {
        UUID suggestionId = createSuggestion(firstUserId, "Dropbox", "USD", "11.9900");
        Integer before = subscriptionCount(firstUserId);

        mockMvc.perform(post("/inbound-email/suggestions/{id}/ignore", suggestionId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isNoContent());

        assertEquals("IGNORED", suggestionStatus(suggestionId));
        assertEquals(before, subscriptionCount(firstUserId));
        mockMvc.perform(post("/inbound-email/suggestions/{id}/ignore", suggestionId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void suggestionEndpointsRequireAuthentication() throws Exception {
        UUID suggestionId = UUID.randomUUID();
        mockMvc.perform(get("/inbound-email/suggestions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/inbound-email/suggestions/{id}/ignore", suggestionId))
                .andExpect(status().isUnauthorized());
    }

    private UUID createSuggestion(
            long userId, String provider, String currency, String amount) {
        long addressId = jdbc.queryForObject("""
                INSERT INTO inbound_email_address (
                    user_id, token_hash, encrypted_token, created_at
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, userId, UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", ""),
                "encrypted-" + UUID.randomUUID());
        UUID emailId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inbound_email (
                    id, user_id, recipient_address_id, message_fingerprint,
                    status, received_at
                ) VALUES (?, ?, ?, ?, 'SUGGESTION_CREATED', CURRENT_TIMESTAMP)
                """, emailId, userId, addressId,
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""));
        UUID suggestionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO subscription_suggestion (
                    id, user_id, inbound_email_id, provider, amount, currency,
                    event_type, confidence, evidence_summary, status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?::numeric, ?, 'RENEWAL_PAYMENT',
                    0.9000, 'provider,event,money', 'PENDING',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, suggestionId, userId, emailId, provider, amount, currency);
        return suggestionId;
    }

    private long createUser(String suffix) {
        return jdbc.queryForObject("""
                INSERT INTO users (google_id, email, name)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, USER_PREFIX + suffix,
                suffix + "@example.com", "Suggestion " + suffix);
    }

    private String accessToken(String googleId) {
        User user = new User();
        user.setGoogleId(googleId);
        return jwtService.generateAccessToken(user);
    }

    private String suggestionStatus(UUID suggestionId) {
        return jdbc.queryForObject(
                "SELECT status FROM subscription_suggestion WHERE id = ?",
                String.class, suggestionId);
    }

    private Integer subscriptionCount(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM subscription WHERE user_id = ?",
                Integer.class, userId);
    }

    private String confirmRequest() {
        return """
                {
                  "name": "Netflix Premium",
                  "cost": 12.9900,
                  "currency": "EUR",
                  "type": "one-time",
                  "category": "Entertainment",
                  "startDate": "2026-07-28",
                  "emailNotificationsEnabled": false
                }
                """;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
