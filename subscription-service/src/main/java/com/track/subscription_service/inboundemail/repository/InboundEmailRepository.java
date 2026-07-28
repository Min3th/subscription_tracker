package com.track.subscription_service.inboundemail.repository;

import com.track.subscription_service.inboundemail.entity.InboundEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface InboundEmailRepository extends JpaRepository<InboundEmail, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO inbound_email (
                id, user_id, recipient_address_id, provider_message_id,
                message_fingerprint, envelope_from, subject, text_body,
                html_body, raw_headers, spam_score, status, attempt_count, received_at
            ) VALUES (
                :id, :userId, :addressId, :providerMessageId,
                :fingerprint, :envelopeFrom, :subject, :textBody,
                :htmlBody, :rawHeaders, :spamScore, 'RECEIVED', 0, :receivedAt
            )
            ON CONFLICT (recipient_address_id, message_fingerprint) DO NOTHING
            """, nativeQuery = true)
    int insertReceived(
            @Param("id") UUID id,
            @Param("userId") Long userId,
            @Param("addressId") Long addressId,
            @Param("providerMessageId") String providerMessageId,
            @Param("fingerprint") String fingerprint,
            @Param("envelopeFrom") String envelopeFrom,
            @Param("subject") String subject,
            @Param("textBody") String textBody,
            @Param("htmlBody") String htmlBody,
            @Param("rawHeaders") String rawHeaders,
            @Param("spamScore") BigDecimal spamScore,
            @Param("receivedAt") Instant receivedAt
    );

    @Modifying
    @Query(value = """
            WITH expired AS (
                SELECT id
                FROM inbound_email
                WHERE received_at < :cutoff
                  AND content_purged_at IS NULL
                ORDER BY received_at
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            )
            UPDATE inbound_email email
            SET text_body = NULL,
                html_body = NULL,
                raw_headers = NULL,
                content_purged_at = :purgedAt,
                status = CASE
                    WHEN email.status IN ('RECEIVED', 'PROCESSING', 'RETRY') THEN 'DEAD'
                    ELSE email.status
                END,
                failure_code = CASE
                    WHEN email.status IN ('RECEIVED', 'PROCESSING', 'RETRY')
                        THEN 'CONTENT_RETENTION_EXPIRED'
                    ELSE email.failure_code
                END,
                next_attempt_at = NULL,
                completed_at = COALESCE(email.completed_at, :purgedAt)
            FROM expired
            WHERE email.id = expired.id
            """, nativeQuery = true)
    int purgeExpiredContent(
            @Param("cutoff") Instant cutoff,
            @Param("purgedAt") Instant purgedAt,
            @Param("batchSize") int batchSize
    );
}
