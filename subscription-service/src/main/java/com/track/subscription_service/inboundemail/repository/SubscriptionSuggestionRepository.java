package com.track.subscription_service.inboundemail.repository;

import com.track.subscription_service.inboundemail.entity.SubscriptionSuggestion;
import com.track.subscription_service.inboundemail.model.SuggestionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

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
}
