package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.entity.SubscriptionReminderSchedule;
import com.track.subscription_service.notification.repository.SubscriptionReminderScheduleRepository;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.model.SubscriptionType;
import com.track.subscription_service.subscription.service.BillingService;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.entity.UserPreferences;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReminderScheduleServiceTest {

    @Test
    void persistsTimezoneAdjustedDueInstant() {
        SubscriptionReminderScheduleRepository repository = mock(SubscriptionReminderScheduleRepository.class);
        when(repository.findById(42L)).thenReturn(Optional.empty());
        Clock clock = Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC);
        ReminderScheduleService service = new ReminderScheduleService(repository, new BillingService(), clock);

        service.refresh(subscription(true));

        ArgumentCaptor<SubscriptionReminderSchedule> captor = ArgumentCaptor.forClass(SubscriptionReminderSchedule.class);
        verify(repository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 7, 25), captor.getValue().getBillingDate());
        assertEquals(Instant.parse("2026-07-22T03:30:00Z"), captor.getValue().getDueAt());
    }

    @Test
    void removesScheduleWhenSubscriptionOptOutIsDisabled() {
        SubscriptionReminderScheduleRepository repository = mock(SubscriptionReminderScheduleRepository.class);
        ReminderScheduleService service = new ReminderScheduleService(
                repository, new BillingService(), Clock.systemUTC()
        );

        service.refresh(subscription(false));

        verify(repository).deleteById(42L);
        verify(repository, never()).save(any());
    }

    private Subscription subscription(boolean enabled) {
        User user = new User();
        UserPreferences preferences = new UserPreferences();
        preferences.setUser(user);
        preferences.setTimezone("Asia/Colombo");
        preferences.setReminderTime(LocalTime.of(9, 0));
        preferences.setReminderDaysBefore(3);
        preferences.setEmailNotificationsEnabled(true);
        user.setPreferences(preferences);

        Subscription subscription = new Subscription();
        subscription.setId(42L);
        subscription.setUser(user);
        subscription.setType(SubscriptionType.RECURRING);
        subscription.setStartDate(LocalDate.of(2026, 1, 25));
        subscription.setBillingIntervalUnit(BillingUnit.MONTH);
        subscription.setBillingIntervalCount(1);
        subscription.setEmailNotificationsEnabled(enabled);
        return subscription;
    }
}
