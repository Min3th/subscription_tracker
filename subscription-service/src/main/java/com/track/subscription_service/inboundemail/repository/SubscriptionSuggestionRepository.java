package com.track.subscription_service.inboundemail.repository;

import com.track.subscription_service.inboundemail.entity.SubscriptionSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriptionSuggestionRepository
        extends JpaRepository<SubscriptionSuggestion, UUID> {
    boolean existsByInboundEmailId(UUID inboundEmailId);
}
