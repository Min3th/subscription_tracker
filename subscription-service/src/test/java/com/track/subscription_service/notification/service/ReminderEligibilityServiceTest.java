package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.model.SubscriptionType;
import com.track.subscription_service.subscription.service.BillingService;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.entity.UserPreferences;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class ReminderEligibilityServiceTest {

    @Test
    void usesUserTimezoneRatherThanServerTimezone() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-22T03:31:00Z"), ZoneOffset.UTC);
        ReminderEligibilityService service = new ReminderEligibilityService(new BillingService(), clock);
        Subscription subscription = subscription("Asia/Colombo", LocalTime.of(9, 0), true, true, 3);

        assertEquals(LocalDate.of(2026, 7, 25), service.dueBillingDate(subscription).orElseThrow());

        subscription.getUser().getPreferences().setTimezone("America/New_York");
        assertTrue(service.dueBillingDate(subscription).isEmpty());
    }

    @Test
    void requiresBothAccountAndSubscriptionConsent() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-22T03:31:00Z"), ZoneOffset.UTC);
        ReminderEligibilityService service = new ReminderEligibilityService(new BillingService(), clock);

        assertTrue(service.dueBillingDate(
                subscription("Asia/Colombo", LocalTime.of(9, 0), true, false, 3)
        ).isEmpty());
        assertTrue(service.dueBillingDate(
                subscription("Asia/Colombo", LocalTime.of(9, 0), false, true, 3)
        ).isEmpty());
    }

    @Test
    void fiveMinuteWindowWorksAcrossLocalMidnight() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-22T18:31:00Z"), ZoneOffset.UTC);
        ReminderEligibilityService service = new ReminderEligibilityService(new BillingService(), clock);
        Subscription subscription = subscription("Asia/Colombo", LocalTime.of(23, 58), true, true, 0);
        subscription.setStartDate(LocalDate.of(2026, 7, 22));

        assertEquals(LocalDate.of(2026, 7, 22), service.dueBillingDate(subscription).orElseThrow());
    }

    @Test
    void invalidTimezoneDoesNotFallBackToServerTime() {
        ReminderEligibilityService service = new ReminderEligibilityService(
                new BillingService(), Clock.fixed(Instant.parse("2026-07-22T03:31:00Z"), ZoneOffset.UTC)
        );
        assertTrue(service.dueBillingDate(
                subscription("Not/A-Timezone", LocalTime.of(9, 0), true, true, 3)
        ).isEmpty());
    }

    private Subscription subscription(String timezone, LocalTime reminderTime,
                                      boolean accountEnabled, boolean subscriptionEnabled,
                                      int reminderDays) {
        User user = new User();
        UserPreferences preferences = new UserPreferences();
        preferences.setUser(user);
        preferences.setTimezone(timezone);
        preferences.setReminderTime(reminderTime);
        preferences.setReminderDaysBefore(reminderDays);
        preferences.setEmailNotificationsEnabled(accountEnabled);
        user.setPreferences(preferences);

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setType(SubscriptionType.RECURRING);
        subscription.setStartDate(LocalDate.of(2026, 1, 25));
        subscription.setBillingIntervalUnit(BillingUnit.MONTH);
        subscription.setBillingIntervalCount(1);
        subscription.setEmailNotificationsEnabled(subscriptionEnabled);
        return subscription;
    }
}
