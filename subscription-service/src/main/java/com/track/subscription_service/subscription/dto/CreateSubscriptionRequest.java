package com.track.subscription_service.subscription.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.model.SubscriptionCategory;
import com.track.subscription_service.subscription.model.SubscriptionType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record CreateSubscriptionRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Positive Double cost,
        @NotNull SubscriptionType type,
        @Size(max = 50) String duration,
        @NotNull SubscriptionCategory category,
        @Size(max = 1000) String description,
        @Size(max = 120) String paymentMethod,
        @Size(max = 500) @URL(regexp = "^https?://.*$") String website,
        @NotNull LocalDate startDate,
        BillingUnit billingIntervalUnit,
        @Positive Integer billingIntervalCount,
        boolean emailNotificationsEnabled
) {
    public CreateSubscriptionRequest {
        name = normalizeRequired(name);
        duration = normalizeOptional(duration);
        description = normalizeOptional(description);
        paymentMethod = normalizeOptional(paymentMethod);
        website = normalizeOptional(website);
    }

    @AssertTrue(message = "Recurring subscriptions require a billing interval")
    public boolean isBillingIntervalValid() {
        return type != SubscriptionType.RECURRING
                || (billingIntervalUnit != null && billingIntervalCount != null);
    }

    private static String normalizeRequired(String value) { return value == null ? null : value.trim(); }
    private static String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
