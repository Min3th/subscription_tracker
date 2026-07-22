package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.entity.SubscriptionReminderSchedule;
import com.track.subscription_service.notification.repository.SubscriptionReminderScheduleRepository;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.mockito.Mockito.*;

class SchedulerServiceTest {

    @Test
    void fetchesOnlyBoundedDueSchedulesAndAdvancesThem() {
        SubscriptionReminderScheduleRepository repository = mock(SubscriptionReminderScheduleRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        ReminderScheduleService scheduleService = mock(ReminderScheduleService.class);
        Instant now = Instant.parse("2026-07-22T03:30:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        User user = new User();
        Subscription subscription = new Subscription();
        subscription.setId(42L);
        subscription.setUser(user);
        SubscriptionReminderSchedule schedule = new SubscriptionReminderSchedule();
        schedule.setSubscription(subscription);
        schedule.setBillingDate(LocalDate.of(2026, 7, 25));
        schedule.setDueAt(now);
        schedule.setUpdatedAt(now);
        when(repository.findTop100ByDueAtLessThanEqualOrderByDueAtAsc(now)).thenReturn(List.of(schedule));

        SchedulerService scheduler = new SchedulerService(repository, notificationService, scheduleService, clock);
        scheduler.checkSubscriptions();

        verify(repository).findTop100ByDueAtLessThanEqualOrderByDueAtAsc(now);
        verify(notificationService).sendSubscriptionReminder(user, subscription, LocalDate.of(2026, 7, 25));
        verify(scheduleService).advanceAfterDelivery(subscription, LocalDate.of(2026, 7, 25));
    }

    @Test
    void oneUnexpectedFailureDoesNotStopTheBatch() {
        SubscriptionReminderScheduleRepository repository = mock(SubscriptionReminderScheduleRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        ReminderScheduleService scheduleService = mock(ReminderScheduleService.class);
        Instant now = Instant.parse("2026-07-22T03:30:00Z");

        Subscription first = subscription(1L);
        Subscription second = subscription(2L);
        SubscriptionReminderSchedule firstSchedule = schedule(first, now);
        SubscriptionReminderSchedule secondSchedule = schedule(second, now);
        when(repository.findTop100ByDueAtLessThanEqualOrderByDueAtAsc(now))
                .thenReturn(List.of(firstSchedule, secondSchedule));
        when(notificationService.sendSubscriptionReminder(any(), eq(first), any()))
                .thenThrow(new RuntimeException("unexpected"));

        new SchedulerService(repository, notificationService, scheduleService,
                Clock.fixed(now, ZoneOffset.UTC)).checkSubscriptions();

        verify(notificationService).sendSubscriptionReminder(any(), eq(second), any());
        verify(scheduleService).advanceAfterDelivery(first, LocalDate.of(2026, 7, 25));
        verify(scheduleService).advanceAfterDelivery(second, LocalDate.of(2026, 7, 25));
    }

    private static Subscription subscription(Long id) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setUser(new User());
        return subscription;
    }

    private static SubscriptionReminderSchedule schedule(Subscription subscription, Instant dueAt) {
        SubscriptionReminderSchedule schedule = new SubscriptionReminderSchedule();
        schedule.setSubscription(subscription);
        schedule.setBillingDate(LocalDate.of(2026, 7, 25));
        schedule.setDueAt(dueAt);
        schedule.setUpdatedAt(dueAt);
        return schedule;
    }
}
