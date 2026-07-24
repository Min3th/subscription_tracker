package com.track.subscription_service.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "google.client.id=test-google-client-id",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.sendgrid.eventWebhookPublicKey="
})
class DatabaseMigrationIntegrationTest extends PostgresIntegrationTest {

    private static final String TEST_GOOGLE_ID = "database-migration-test-user";

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id = ?", TEST_GOOGLE_ID);
    }

    @Test
    void flywayBuildsTheCompleteSchemaFromAnEmptyDatabase() {
        Integer failedMigrations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE",
                Integer.class);
        String currentVersion = jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE "
                        + "AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1",
                String.class);

        assertEquals(0, failedMigrations);
        assertEquals("8", currentVersion);
        assertNotNull(regclass("users"));
        assertNotNull(regclass("subscription"));
        assertNotNull(regclass("notification_delivery"));
        assertNotNull(regclass("subscription_reminder_schedule"));
        assertNotNull(regclass("refresh_token_sessions"));
    }

    @Test
    void subscriptionRejectsNullRequiredFields() {
        long userId = createUser();

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    email_notifications_enabled, user_id
                ) VALUES (NULL, 10.0000, 'USD', 'ONE_TIME', 'SOFTWARE', CURRENT_DATE, FALSE, ?)
                """, userId));
    }

    @Test
    void subscriptionRejectsInvalidFinancialValues() {
        long userId = createUser();

        assertThrows(DataIntegrityViolationException.class,
                () -> insertOneTime(userId, "Invalid cost", "0.0000", "USD"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertOneTime(userId, "Invalid currency", "10.0000", "usd"));
    }

    @Test
    void subscriptionRejectsUnsupportedEnumValues() {
        long userId = createUser();

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    email_notifications_enabled, user_id
                ) VALUES ('Invalid type', 10.0000, 'USD', 'TRIAL', 'SOFTWARE', CURRENT_DATE, FALSE, ?)
                """, userId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    email_notifications_enabled, user_id
                ) VALUES ('Invalid category', 10.0000, 'USD', 'ONE_TIME', 'UNKNOWN', CURRENT_DATE, FALSE, ?)
                """, userId));
    }

    @Test
    void subscriptionEnforcesBillingRulesForEachType() {
        long userId = createUser();

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    billing_interval_unit, billing_interval_count,
                    email_notifications_enabled, user_id
                ) VALUES ('Missing interval', 10.0000, 'USD', 'RECURRING', 'SOFTWARE',
                    CURRENT_DATE, NULL, NULL, FALSE, ?)
                """, userId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    billing_interval_unit, billing_interval_count,
                    email_notifications_enabled, user_id
                ) VALUES ('Zero interval', 10.0000, 'USD', 'RECURRING', 'SOFTWARE',
                    CURRENT_DATE, 'MONTH', 0, FALSE, ?)
                """, userId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    billing_interval_unit, billing_interval_count,
                    email_notifications_enabled, user_id
                ) VALUES ('One-time interval', 10.0000, 'USD', 'ONE_TIME', 'SOFTWARE',
                    CURRENT_DATE, 'MONTH', 1, FALSE, ?)
                """, userId));
    }

    private long createUser() {
        jdbc.update(
                "INSERT INTO users (google_id, email, name) VALUES (?, ?, ?)",
                TEST_GOOGLE_ID, "migration-test@example.com", "Migration Test");
        return jdbc.queryForObject(
                "SELECT id FROM users WHERE google_id = ?",
                Long.class,
                TEST_GOOGLE_ID);
    }

    private void insertOneTime(long userId, String name, String cost, String currency) {
        jdbc.update("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    email_notifications_enabled, user_id
                ) VALUES (?, ?::numeric, ?, 'ONE_TIME', 'SOFTWARE', CURRENT_DATE, FALSE, ?)
                """, name, cost, currency, userId);
    }

    private String regclass(String tableName) {
        return jdbc.queryForObject("SELECT to_regclass(?)::text", String.class, "public." + tableName);
    }
}
