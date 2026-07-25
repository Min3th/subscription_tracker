package com.track.subscription_service.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.Currency;

public record UpdateUserPreferencesRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotBlank @Size(max = 35)
        @Pattern(regexp = "^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$") String language,
        @NotBlank @Size(max = 50) String timezone,
        @NotBlank @Pattern(regexp = "^(light|dark)$") String theme,
        @NotNull Boolean emailNotificationsEnabled,
        @Min(0) @Max(365) int reminderDaysBefore,
        @NotNull LocalTime reminderTime
) {
    public UpdateUserPreferencesRequest {
        currency = currency == null ? null : currency.trim().toUpperCase();
        language = language == null ? null : language.trim();
        timezone = timezone == null ? null : timezone.trim();
        theme = theme == null ? null : theme.trim().toLowerCase();
    }

    @AssertTrue(message = "Currency must be a supported ISO 4217 code")
    public boolean isCurrencySupported() {
        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            return true;
        }
        try {
            Currency.getInstance(currency);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @AssertTrue(message = "Timezone must be a valid IANA timezone")
    public boolean isTimezoneSupported() {
        if (timezone == null || timezone.isBlank()) {
            return true;
        }
        try {
            ZoneId.of(timezone);
            return true;
        } catch (DateTimeException exception) {
            return false;
        }
    }
}
