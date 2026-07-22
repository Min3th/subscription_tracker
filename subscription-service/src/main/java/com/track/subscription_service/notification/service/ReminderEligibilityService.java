package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.model.SubscriptionType;
import com.track.subscription_service.subscription.service.BillingService;
import com.track.subscription_service.user.entity.UserPreferences;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Optional;

@Service
public class ReminderEligibilityService {

    private static final Duration DELIVERY_WINDOW = Duration.ofMinutes(5);

    private final BillingService billingService;
    private final Clock clock;

    public ReminderEligibilityService(BillingService billingService, Clock clock) {
        this.billingService = billingService;
        this.clock = clock;
    }

    public Optional<LocalDate> dueBillingDate(Subscription subscription) {
        UserPreferences preferences = subscription.getUser().getPreferences();
        if (preferences == null
                || subscription.getType() != SubscriptionType.RECURRING
                || !Boolean.TRUE.equals(preferences.getEmailNotificationsEnabled())
                || !Boolean.TRUE.equals(subscription.isEmailNotificationsEnabled())) {
            return Optional.empty();
        }

        ZoneId userZone;
        try {
            userZone = ZoneId.of(preferences.getTimezone() == null ? "UTC" : preferences.getTimezone());
        } catch (DateTimeException invalidTimezone) {
            return Optional.empty();
        }

        Instant currentInstant = clock.instant();
        ZonedDateTime userNow = currentInstant.atZone(userZone);
        LocalDate eligibilityDate = userNow.minus(DELIVERY_WINDOW).toLocalDate();
        LocalDate billingDate = billingService.getBillingDateOnOrAfter(
                subscription.getStartDate(),
                subscription.getBillingIntervalUnit(),
                subscription.getBillingIntervalCount(),
                eligibilityDate
        );

        LocalDate reminderDate = billingDate.minusDays(preferences.getReminderDaysBefore());
        LocalTime reminderTime = preferences.getReminderTime() == null
                ? LocalTime.of(9, 0) : preferences.getReminderTime();
        Instant windowStart = reminderDate.atTime(reminderTime).atZone(userZone).toInstant();

        if (currentInstant.isBefore(windowStart)
                || !currentInstant.isBefore(windowStart.plus(DELIVERY_WINDOW))) {
            return Optional.empty();
        }
        return Optional.of(billingDate);
    }
}
