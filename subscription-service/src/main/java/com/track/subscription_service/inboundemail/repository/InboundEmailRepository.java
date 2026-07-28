package com.track.subscription_service.inboundemail.repository;

import com.track.subscription_service.inboundemail.entity.InboundEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InboundEmailRepository extends JpaRepository<InboundEmail, UUID> {

    @Modifying
    @Transactional
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

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE inbound_email
            SET status = 'PROCESSING',
                attempt_count = attempt_count + 1,
                processing_started_at = :claimedAt,
                next_attempt_at = NULL,
                claim_token = :claimToken
            WHERE id IN (
                SELECT id
                FROM inbound_email
                WHERE (status = 'RECEIVED')
                   OR (status = 'RETRY' AND next_attempt_at <= :claimedAt)
                   OR (status = 'PROCESSING' AND processing_started_at <= :staleBefore)
                ORDER BY COALESCE(next_attempt_at, received_at)
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            )
            """, nativeQuery = true)
    int claimProcessingBatch(
            @Param("claimToken") String claimToken,
            @Param("claimedAt") Instant claimedAt,
            @Param("staleBefore") Instant staleBefore,
            @Param("batchSize") int batchSize
    );

    @Query("""
            SELECT email
            FROM InboundEmail email
            JOIN FETCH email.user
            WHERE email.claimToken = :claimToken
            ORDER BY email.receivedAt
            """)
    List<InboundEmail> findClaimedBatch(@Param("claimToken") String claimToken);

    @Modifying
    @Transactional
    @Query("""
            UPDATE InboundEmail email
            SET email.status = :status,
                email.completedAt = :completedAt,
                email.failureCode = null,
                email.nextAttemptAt = null,
                email.claimToken = null
            WHERE email.id = :id AND email.claimToken = :claimToken
            """)
    int markCompleted(
            @Param("id") UUID id,
            @Param("claimToken") String claimToken,
            @Param("status") com.track.subscription_service.inboundemail.model.InboundEmailStatus status,
            @Param("completedAt") Instant completedAt
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE InboundEmail email
            SET email.status = :status,
                email.failureCode = :failureCode,
                email.nextAttemptAt = :nextAttemptAt,
                email.completedAt = :completedAt,
                email.claimToken = null
            WHERE email.id = :id AND email.claimToken = :claimToken
            """)
    int markFailed(
            @Param("id") UUID id,
            @Param("claimToken") String claimToken,
            @Param("status") com.track.subscription_service.inboundemail.model.InboundEmailStatus status,
            @Param("failureCode") String failureCode,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("completedAt") Instant completedAt
    );
}
