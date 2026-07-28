package com.track.subscription_service.inboundemail.dto;

import com.track.subscription_service.inboundemail.model.InboundEmailClassification;
import com.track.subscription_service.subscription.model.BillingUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionExtraction(
        InboundEmailClassification classification,
        String provider,
        String planName,
        BigDecimal amount,
        String currency,
        BillingUnit billingIntervalUnit,
        Integer billingIntervalCount,
        LocalDate renewalDate,
        BigDecimal confidence,
        String evidenceSummary,
        String actionUrl
) {
    public SubscriptionExtraction(
            InboundEmailClassification classification,
            String provider,
            String planName,
            BigDecimal amount,
            String currency,
            BillingUnit billingIntervalUnit,
            Integer billingIntervalCount,
            LocalDate renewalDate,
            BigDecimal confidence,
            String evidenceSummary) {
        this(classification, provider, planName, amount, currency,
                billingIntervalUnit, billingIntervalCount, renewalDate,
                confidence, evidenceSummary, null);
    }

    public boolean isSuggestionCandidate() {
        return classification != InboundEmailClassification.NOT_SUBSCRIPTION
                && provider != null;
    }
}
