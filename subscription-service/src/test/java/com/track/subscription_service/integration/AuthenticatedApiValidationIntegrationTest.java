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
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.hasItem;
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
class AuthenticatedApiValidationIntegrationTest extends PostgresIntegrationTest {

    private static final String GOOGLE_ID = "validation-api-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private String accessToken;

    @BeforeEach
    void createUser() {
        jdbc.update("""
                INSERT INTO users (google_id, email, name)
                VALUES (?, 'validation@example.com', 'Validation User')
                """, GOOGLE_ID);
        accessToken = accessToken(GOOGLE_ID);
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id = ?", GOOGLE_ID);
    }

    @Test
    void invalidSubscriptionReturnsStableFieldValidationErrors() throws Exception {
        String invalidRequest = """
                {
                  "name": "   ",
                  "cost": -1.25,
                  "currency": "not-a-currency",
                  "type": "RECURRING",
                  "category": "SOFTWARE",
                  "website": "javascript:alert(1)",
                  "startDate": "2026-01-01",
                  "billingIntervalCount": 0,
                  "emailNotificationsEnabled": true
                }
                """;

        mockMvc.perform(post("/subscriptions")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/subscriptions"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("cost")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("currency")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("website")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("billingIntervalCount")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("billingIntervalValid")));
    }

    @Test
    void malformedJsonReturnsStableErrorDocument() throws Exception {
        mockMvc.perform(post("/subscriptions")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.path").value("/subscriptions"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void firstPreferenceReadCreatesAndReturnsStableDefaults() throws Exception {
        mockMvc.perform(get("/user/preferences")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.timezone").value("UTC"))
                .andExpect(jsonPath("$.theme").value("light"))
                .andExpect(jsonPath("$.emailNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.reminderDaysBefore").value(3))
                .andExpect(jsonPath("$.reminderTime").value("09:00:00"));

        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_preferences preferences
                JOIN users users ON users.id = preferences.user_id
                WHERE users.google_id = ?
                """, Integer.class, GOOGLE_ID));
    }

    @Test
    void invalidPreferencesAreRejectedWithoutChangingStoredDefaults() throws Exception {
        getPreferences();
        String invalidRequest = """
                {
                  "currency": "ZZZ",
                  "language": "!",
                  "timezone": "Mars/Olympus",
                  "theme": "neon",
                  "emailNotificationsEnabled": null,
                  "reminderDaysBefore": -1,
                  "reminderTime": null
                }
                """;

        mockMvc.perform(put("/user/preferences")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("currencySupported")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("timezoneSupported")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("theme")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("reminderDaysBefore")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("reminderTime")));

        assertEquals("USD", jdbc.queryForObject("""
                SELECT preferences.currency FROM user_preferences preferences
                JOIN users users ON users.id = preferences.user_id
                WHERE users.google_id = ?
                """, String.class, GOOGLE_ID));
    }

    @Test
    void validPreferencesAreNormalizedBeforePersistence() throws Exception {
        String request = """
                {
                  "currency": " lkr ",
                  "language": "si-LK",
                  "timezone": " Asia/Colombo ",
                  "theme": " DARK ",
                  "emailNotificationsEnabled": false,
                  "reminderDaysBefore": 5,
                  "reminderTime": "08:30:00"
                }
                """;

        mockMvc.perform(put("/user/preferences")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("LKR"))
                .andExpect(jsonPath("$.timezone").value("Asia/Colombo"))
                .andExpect(jsonPath("$.theme").value("dark"));
    }

    @Test
    void unknownAuthenticatedUserGetsNotFoundErrorRatherThanInternalError() throws Exception {
        mockMvc.perform(get("/user/preferences")
                        .header("Authorization", bearer(accessToken("unknown-validation-user"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/user/preferences"));
    }

    @Test
    void missingRefreshCookieReturnsStableUnauthorizedError() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"))
                .andExpect(jsonPath("$.message").value("No refresh token available"))
                .andExpect(jsonPath("$.path").value("/auth/refresh"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void invalidRefreshCookieReturnsStableUnauthorizedErrorAndIsCleared() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "not-a-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                .andExpect(jsonPath("$.path").value("/auth/refresh"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string(HttpHeaders.SET_COOKIE,
                                org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    private void getPreferences() throws Exception {
        mockMvc.perform(get("/user/preferences")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());
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
