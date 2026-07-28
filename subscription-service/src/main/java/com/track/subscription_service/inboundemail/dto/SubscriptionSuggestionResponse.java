package com.track.subscription_service.inboundemail.dto;

import com.track.subscription_service.inboundemail.model.SuggestionEventType;
import com.track.subscription_service.inboundemail.model.SuggestionStatus;
import com.track.subscription_service.subscription.model.BillingUnit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionSuggestionResponse(
        UUID id,
        String provider,
        String planName,
        BigDecimal amount,
        String currency,
        BillingUnit billingIntervalUnit,
        Integer billingIntervalCount,
        LocalDate renewalDate,
        SuggestionEventType eventType,
        BigDecimal confidence,
        String evidenceSummary,
        String actionUrl,
        SuggestionStatus status,
        PossibleDuplicate possibleDuplicate,
        Instant receivedAt,
        Instant createdAt
) {
    public record PossibleDuplicate(Long subscriptionId, String name) {
    }
}
