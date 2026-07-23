package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.repository.*;
import com.track.subscription_service.user.entity.*;
import com.track.subscription_service.user.repository.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class EmailSuppressionServiceTest {
    @Test
    void suppressionDisablesPreferencesSchedulesAndQueuedDeliveries() {
        EmailSuppressionRepository suppressions = mock(EmailSuppressionRepository.class);
        UserRepository users = mock(UserRepository.class);
        UserPreferencesRepository preferences = mock(UserPreferencesRepository.class);
        SubscriptionReminderScheduleRepository schedules = mock(SubscriptionReminderScheduleRepository.class);
        NotificationDeliveryRepository deliveries = mock(NotificationDeliveryRepository.class);
        Instant now = Instant.parse("2026-07-22T03:30:00Z");
        User user = new User();
        user.setId(4L);
        UserPreferences prefs = new UserPreferences();
        prefs.setEmailNotificationsEnabled(true);
        when(users.findAllByEmailIgnoreCase("user@example.com")).thenReturn(List.of(user));
        when(preferences.findByUser(user)).thenReturn(Optional.of(prefs));

        new EmailSuppressionService(suppressions, users, preferences, schedules, deliveries,
                Clock.fixed(now, ZoneOffset.UTC))
                .suppress(" User@Example.com ", "BOUNCE", "SENDGRID");

        verify(suppressions).upsert("user@example.com", "BOUNCE", "SENDGRID", now);
        assertFalse(prefs.getEmailNotificationsEnabled());
        verify(preferences).save(prefs);
        verify(schedules).deleteAllByUserId(4L);
        verify(deliveries).markOpenDeliveriesDeadForUser(4L, now, "Email suppressed: BOUNCE");
    }
}
