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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AccountTenantIsolationApiIntegrationTest extends PostgresIntegrationTest {

    private static final String USER_PREFIX = "account-api-tenant-";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private long firstUserId;
    private long secondUserId;
    private String firstUserToken;

    @BeforeEach
    void createTwoTenants() {
        firstUserId = createUser("first");
        secondUserId = createUser("second");
        createPreferences(firstUserId, "USD", "UTC");
        createPreferences(secondUserId, "EUR", "Europe/Paris");
        firstUserToken = accessToken(USER_PREFIX + "first");
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id LIKE ?", USER_PREFIX + "%");
    }

    @Test
    void preferenceReadUsesTheAuthenticatedIdentity() throws Exception {
        mockMvc.perform(get("/user/preferences")
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(firstUserId))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.timezone").value("UTC"));
    }

    @Test
    void preferenceUpdateCannotBeRedirectedByAUserIdInTheBody() throws Exception {
        String attemptedCrossTenantUpdate = """
                {
                  "userId": %d,
                  "currency": "LKR",
                  "language": "si",
                  "timezone": "Asia/Colombo",
                  "theme": "dark",
                  "emailNotificationsEnabled": false,
                  "reminderDaysBefore": 5,
                  "reminderTime": "08:30:00"
                }
                """.formatted(secondUserId);

        mockMvc.perform(put("/user/preferences")
                        .header("Authorization", bearer(firstUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attemptedCrossTenantUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(firstUserId))
                .andExpect(jsonPath("$.currency").value("LKR"));

        assertEquals("LKR", preferenceCurrency(firstUserId));
        assertEquals("EUR", preferenceCurrency(secondUserId));
    }

    @Test
    void logoutAllRevokesOnlyTheAuthenticatedTenantsSessions() throws Exception {
        createRefreshSession(firstUserId, 'a');
        createRefreshSession(firstUserId, 'b');
        createRefreshSession(secondUserId, 'c');

        mockMvc.perform(post("/auth/logout-all")
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isOk());

        assertEquals(2, revokedSessionCount(firstUserId));
        assertEquals(0, revokedSessionCount(secondUserId));
        assertEquals(1, activeSessionCount(secondUserId));
    }

    private long createUser(String suffix) {
        String googleId = USER_PREFIX + suffix;
        return jdbc.queryForObject("""
                INSERT INTO users (google_id, email, name)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, googleId, suffix + "@example.com", "Tenant " + suffix);
    }

    private void createPreferences(long userId, String currency, String timezone) {
        jdbc.update("""
                INSERT INTO user_preferences (
                    user_id, currency, language, timezone, theme,
                    email_notifications_enabled, reminder_days_before, reminder_time
                ) VALUES (?, ?, 'en', ?, 'light', TRUE, 3, '09:00:00')
                """, userId, currency, timezone);
    }

    private void createRefreshSession(long userId, char hashCharacter) {
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO refresh_token_sessions (
                    token_id, token_hash, user_id, created_at, expires_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID().toString(),
                String.valueOf(hashCharacter).repeat(64),
                userId,
                Timestamp.from(now),
                Timestamp.from(now.plus(7, ChronoUnit.DAYS)));
    }

    private String preferenceCurrency(long userId) {
        return jdbc.queryForObject(
                "SELECT currency FROM user_preferences WHERE user_id = ?",
                String.class,
                userId);
    }

    private int revokedSessionCount(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_token_sessions WHERE user_id = ? AND revoked_at IS NOT NULL",
                Integer.class,
                userId);
    }

    private int activeSessionCount(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_token_sessions WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class,
                userId);
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
