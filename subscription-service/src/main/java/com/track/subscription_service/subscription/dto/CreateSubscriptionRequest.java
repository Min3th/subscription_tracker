package com.track.subscription_service.subscription.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSubscriptionRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Positive Double cost,
        @NotBlank @Pattern(regexp = "one-time|recurring") String type,
        @Size(max = 50) String duration,
        @NotBlank @Size(max = 80) String category,
        @Size(max = 1000) String description,
        @Size(max = 120) String paymentMethod,
        @Size(max = 500) String website,
        @NotNull LocalDate startDate,
        @Pattern(regexp = "day|week|month|year") String billingIntervalUnit,
        @Positive Integer billingIntervalCount,
        boolean emailNotificationsEnabled
) {
    @AssertTrue(message = "Recurring subscriptions require a billing interval")
    public boolean isBillingIntervalValid() {
        return !"recurring".equals(type)
                || (billingIntervalUnit != null && billingIntervalCount != null);
    }
}
