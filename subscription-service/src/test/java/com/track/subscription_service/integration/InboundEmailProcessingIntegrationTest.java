package com.track.subscription_service.integration;

import com.track.subscription_service.inboundemail.service.InboundEmailProcessingWorker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "google.client.id=test-google-client-id",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.sendgrid.eventWebhookPublicKey=",
        "app.inbound-email.processing-initial-delay-ms=3600000"
})
class InboundEmailProcessingIntegrationTest extends PostgresIntegrationTest {
    private static final String GOOGLE_ID = "inbound-processing-integration";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InboundEmailProcessingWorker worker;

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id = ?", GOOGLE_ID);
    }

    @Test
    void receivedEmailBecomesOnePendingSuggestion() {
        long userId = jdbc.queryForObject("""
                INSERT INTO users (google_id, email, name)
                VALUES (?, 'processing@example.com', 'Processing Test')
                RETURNING id
                """, Long.class, GOOGLE_ID);
        long addressId = jdbc.queryForObject("""
                INSERT INTO inbound_email_address (
                    user_id, token_hash, encrypted_token, created_at
                ) VALUES (?, ?, 'encrypted-token', CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, userId, "a".repeat(64));
        UUID emailId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO inbound_email (
                    id, user_id, recipient_address_id, message_fingerprint,
                    envelope_from, subject, text_body, status, attempt_count, received_at
                ) VALUES (?, ?, ?, ?, 'billing@netflix.com',
                    'Your Netflix renewal receipt',
                    'Netflix renewal receipt
                    Plan: Premium
                    Payment received: USD 22.99
                    Billing: monthly
                    Next billing date: August 27, 2026',
                    'RECEIVED', 0, CURRENT_TIMESTAMP)
                """, emailId, userId, addressId, "b".repeat(64));

        worker.processDueEmails();

        assertEquals("SUGGESTION_CREATED", jdbc.queryForObject(
                "SELECT status FROM inbound_email WHERE id = ?",
                String.class, emailId));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM subscription_suggestion
                WHERE inbound_email_id = ? AND user_id = ? AND status = 'PENDING'
                """, Integer.class, emailId, userId));
        assertEquals("Netflix", jdbc.queryForObject("""
                SELECT provider
                FROM subscription_suggestion
                WHERE inbound_email_id = ?
                """, String.class, emailId));
        assertEquals("22.9900", jdbc.queryForObject("""
                SELECT amount::text
                FROM subscription_suggestion
                WHERE inbound_email_id = ?
                """, String.class, emailId));
    }
}
