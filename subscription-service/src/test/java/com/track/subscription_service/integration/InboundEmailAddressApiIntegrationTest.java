package com.track.subscription_service.integration;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "google.client.id=test-google-client-id",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.sendgrid.eventWebhookPublicKey=",
        "app.inbound-email.domain=inbound.test.example",
        "app.inbound-email.token-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class InboundEmailAddressApiIntegrationTest extends PostgresIntegrationTest {
    private static final String USER_PREFIX = "inbound-address-api-";

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
    void addressLifecycleIsAuthenticatedIdempotentAndTenantScoped() throws Exception {
        mockMvc.perform(get("/inbound-email/address")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());

        String firstAddress = createAddress(firstToken);
        assertEquals(firstAddress, createAddress(firstToken));
        String secondAddress = createAddress(secondToken);
        assertNotEquals(firstAddress, secondAddress);

        mockMvc.perform(get("/inbound-email/address")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.address").value(firstAddress));

        String rotatedAddress = mockMvc.perform(post("/inbound-email/address/rotate")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        assertFalse(rotatedAddress.contains(firstAddress));
        assertEquals(1, activeAddressCount(firstUserId));
        assertEquals(2, totalAddressCount(firstUserId));
        assertEquals(1, activeAddressCount(secondUserId));

        mockMvc.perform(delete("/inbound-email/address")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/inbound-email/address")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isNoContent());

        assertEquals(0, activeAddressCount(firstUserId));
        assertEquals(1, activeAddressCount(secondUserId));
    }

    @Test
    void persistedAddressDoesNotStoreTheRawToken() throws Exception {
        String address = createAddress(firstToken);
        String rawToken = address.substring(4, address.indexOf('@'));

        String tokenHash = jdbc.queryForObject(
                "SELECT token_hash FROM inbound_email_address WHERE user_id = ? AND revoked_at IS NULL",
                String.class,
                firstUserId);
        String encryptedToken = jdbc.queryForObject(
                "SELECT encrypted_token FROM inbound_email_address WHERE user_id = ? AND revoked_at IS NULL",
                String.class,
                firstUserId);

        assertNotEquals(rawToken, tokenHash);
        assertFalse(encryptedToken.contains(rawToken));
        assertEquals(64, tokenHash.length());
    }

    @Test
    void lifecycleEndpointRejectsMissingAuthentication() throws Exception {
        mockMvc.perform(get("/inbound-email/address"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(post("/inbound-email/address"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/inbound-email/address/rotate"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/inbound-email/address"))
                .andExpect(status().isUnauthorized());
    }

    private String createAddress(String token) throws Exception {
        String json = mockMvc.perform(post("/inbound-email/address")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.address").value(
                        org.hamcrest.Matchers.matchesPattern(
                                "^sub-[A-Za-z0-9_-]{43}@inbound\\.test\\.example$")))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.address");
    }

    private long createUser(String suffix) {
        return jdbc.queryForObject("""
                INSERT INTO users (google_id, email, name)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, USER_PREFIX + suffix, suffix + "@example.com", "Inbound " + suffix);
    }

    private int activeAddressCount(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM inbound_email_address WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class,
                userId);
    }

    private int totalAddressCount(long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM inbound_email_address WHERE user_id = ?",
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
