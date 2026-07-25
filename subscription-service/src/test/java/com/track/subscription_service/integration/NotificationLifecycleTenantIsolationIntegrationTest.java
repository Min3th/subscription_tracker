package com.track.subscription_service.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class NotificationLifecycleTenantIsolationIntegrationTest extends PostgresIntegrationTest {

    private static final String USER_PREFIX = "notification-lifecycle-tenant-";
    private static final String FIRST_EMAIL = "notification-first@example.com";
    private static final String SECOND_EMAIL = "notification-second@example.com";
    private static final String FIRST_TOKEN = "first-tenant-unsubscribe-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long firstUserId;
    private long secondUserId;
    private long firstSubscriptionId;
    private long secondSubscriptionId;

    @BeforeEach
    void createNotificationStateForTwoTenants() {
        firstUserId = createUser("first", FIRST_EMAIL);
        secondUserId = createUser("second", SECOND_EMAIL);
        createPreferences(firstUserId);
        createPreferences(secondUserId);
        firstSubscriptionId = createSubscription(firstUserId, "First notification subscription");
        secondSubscriptionId = createSubscription(secondUserId, "Second notification subscription");
        createScheduleAndDelivery(firstSubscriptionId);
        createScheduleAndDelivery(secondSubscriptionId);
        createUnsubscribeToken(firstUserId, FIRST_TOKEN);
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id LIKE ?", USER_PREFIX + "%");
        jdbc.update(
                "DELETE FROM email_suppression WHERE email_normalized IN (?, ?)",
                FIRST_EMAIL,
                SECOND_EMAIL);
    }

    @Test
    void unsubscribeTokenAffectsOnlyItsOwnerDespiteInjectedUserId() throws Exception {
        mockMvc.perform(post("/notifications/unsubscribe")
                        .param("token", FIRST_TOKEN)
                        .param("userId", Long.toString(secondUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unsubscribed"));

        assertEquals(false, notificationsEnabled(firstUserId));
        assertEquals(true, notificationsEnabled(secondUserId));
        assertEquals(0, scheduleCount(firstSubscriptionId));
        assertEquals(1, scheduleCount(secondSubscriptionId));
        assertEquals("DEAD", deliveryStatus(firstSubscriptionId));
        assertEquals("PENDING", deliveryStatus(secondSubscriptionId));
        assertEquals(1, suppressionCount(FIRST_EMAIL));
        assertEquals(0, suppressionCount(SECOND_EMAIL));
        assertNotNull(tokenUsedAt(FIRST_TOKEN));
    }

    @Test
    void invalidTokenCannotChangeEitherTenantsNotificationState() throws Exception {
        mockMvc.perform(post("/notifications/unsubscribe")
                        .param("token", "fabricated-token")
                        .param("userId", Long.toString(secondUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"));

        assertNotificationStateUnchanged();
        assertNull(tokenUsedAt(FIRST_TOKEN));
    }

    @Test
    void unsubscribeTokenIsSingleUse() throws Exception {
        mockMvc.perform(post("/notifications/unsubscribe")
                        .param("token", FIRST_TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(post("/notifications/unsubscribe")
                        .param("token", FIRST_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"));

        assertEquals(1, suppressionCount(FIRST_EMAIL));
        assertEquals(0, suppressionCount(SECOND_EMAIL));
        assertEquals("PENDING", deliveryStatus(secondSubscriptionId));
    }

    private void assertNotificationStateUnchanged() {
        assertEquals(true, notificationsEnabled(firstUserId));
        assertEquals(true, notificationsEnabled(secondUserId));
        assertEquals(1, scheduleCount(firstSubscriptionId));
        assertEquals(1, scheduleCount(secondSubscriptionId));
        assertEquals("PENDING", deliveryStatus(firstSubscriptionId));
        assertEquals("PENDING", deliveryStatus(secondSubscriptionId));
        assertEquals(0, suppressionCount(FIRST_EMAIL));
        assertEquals(0, suppressionCount(SECOND_EMAIL));
    }

    private long createUser(String suffix, String email) {
        return jdbc.queryForObject("""
                INSERT INTO users (google_id, email, name)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, USER_PREFIX + suffix, email, "Notification tenant " + suffix);
    }

    private void createPreferences(long userId) {
        jdbc.update("""
                INSERT INTO user_preferences (
                    user_id, currency, language, timezone, theme,
                    email_notifications_enabled, reminder_days_before, reminder_time
                ) VALUES (?, 'USD', 'en', 'UTC', 'light', TRUE, 3, '09:00:00')
                """, userId);
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

    private void createScheduleAndDelivery(long subscriptionId) {
        Instant now = Instant.now();
        LocalDate billingDate = LocalDate.of(2026, 8, 1);
        jdbc.update("""
                INSERT INTO subscription_reminder_schedule (
                    subscription_id, billing_date, due_at, updated_at
                ) VALUES (?, ?, ?, ?)
                """,
                subscriptionId,
                billingDate,
                Timestamp.from(now.plus(1, ChronoUnit.HOURS)),
                Timestamp.from(now));
        jdbc.update("""
                INSERT INTO notification_delivery (
                    subscription_id, billing_date, notification_type, status, attempts,
                    created_at, last_attempt_at
                ) VALUES (?, ?, 'UPCOMING_PAYMENT', 'PENDING', 1, ?, ?)
                """,
                subscriptionId,
                billingDate,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void createUnsubscribeToken(long userId, String rawToken) {
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO notification_unsubscribe_token (
                    user_id, token_hash, created_at, expires_at
                ) VALUES (?, ?, ?, ?)
                """,
                userId,
                hash(rawToken),
                Timestamp.from(now),
                Timestamp.from(now.plus(30, ChronoUnit.DAYS)));
    }

    private Boolean notificationsEnabled(long userId) {
        return jdbc.queryForObject(
                "SELECT email_notifications_enabled FROM user_preferences WHERE user_id = ?",
                Boolean.class,
                userId);
    }

    private int scheduleCount(long subscriptionId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM subscription_reminder_schedule WHERE subscription_id = ?",
                Integer.class,
                subscriptionId);
    }

    private String deliveryStatus(long subscriptionId) {
        return jdbc.queryForObject(
                "SELECT status FROM notification_delivery WHERE subscription_id = ?",
                String.class,
                subscriptionId);
    }

    private int suppressionCount(String email) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM email_suppression WHERE email_normalized = ?",
                Integer.class,
                email);
    }

    private Instant tokenUsedAt(String rawToken) {
        return jdbc.queryForObject(
                "SELECT used_at FROM notification_unsubscribe_token WHERE token_hash = ?",
                (resultSet, rowNumber) -> resultSet.getTimestamp(1) == null
                        ? null : resultSet.getTimestamp(1).toInstant(),
                hash(rawToken));
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
