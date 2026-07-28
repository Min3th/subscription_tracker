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
}
