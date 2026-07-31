package com.track.subscription_service.inboundemail.repository;

import com.track.subscription_service.inboundemail.entity.SubscriptionSuggestion;
import com.track.subscription_service.inboundemail.model.SuggestionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionSuggestionRepository
        extends JpaRepository<SubscriptionSuggestion, UUID> {
    boolean existsByInboundEmailId(UUID inboundEmailId);

    @EntityGraph(attributePaths = {
            "inboundEmail", "possibleDuplicateSubscription", "confirmedSubscription"
    })
    List<SubscriptionSuggestion> findByUser_GoogleIdAndStatusOrderByCreatedAtDesc(
            String googleId, SuggestionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "user", "inboundEmail", "possibleDuplicateSubscription", "confirmedSubscription"
    })
    Optional<SubscriptionSuggestion> findByIdAndUser_GoogleId(
            UUID id, String googleId);

    @Modifying
    @Query(value = """
            WITH expired AS (
                SELECT suggestion.id
                FROM subscription_suggestion suggestion
                JOIN inbound_email email
                  ON email.id = suggestion.inbound_email_id
                WHERE suggestion.action_url IS NOT NULL
                  AND email.received_at < :cutoff
                ORDER BY email.received_at
                LIMIT :batchSize
                FOR UPDATE OF suggestion SKIP LOCKED
            )
            UPDATE subscription_suggestion suggestion
            SET action_url = NULL,
                updated_at = GREATEST(suggestion.updated_at, :purgedAt)
            FROM expired
            WHERE suggestion.id = expired.id
            """, nativeQuery = true)
    int purgeExpiredActionUrls(
            @Param("cutoff") Instant cutoff,
            @Param("purgedAt") Instant purgedAt,
            @Param("batchSize") int batchSize);
}
