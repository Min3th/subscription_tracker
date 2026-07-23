package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.entity.SubscriptionReminderSchedule;
import com.track.subscription_service.notification.repository.SubscriptionReminderScheduleRepository;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.model.SubscriptionType;
import com.track.subscription_service.subscription.service.BillingService;
import com.track.subscription_service.user.entity.UserPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
public class ReminderScheduleService {

    private final SubscriptionReminderScheduleRepository repository;
    private final BillingService billingService;
    private final Clock clock;

    public ReminderScheduleService(SubscriptionReminderScheduleRepository repository,
                                   BillingService billingService, Clock clock) {
        this.repository = repository;
        this.billingService = billingService;
        this.clock = clock;
    }

    @Transactional
    public void refresh(Subscription subscription) {
        if (!isEnabled(subscription)) {
            repository.deleteById(subscription.getId());
            return;
        }

        UserPreferences preferences = subscription.getUser().getPreferences();
        ZoneId zone;
        try {
            zone = ZoneId.of(preferences.getTimezone() == null ? "UTC" : preferences.getTimezone());
        } catch (DateTimeException invalidTimezone) {
            repository.deleteById(subscription.getId());
            return;
        }

        ZonedDateTime userNow = clock.instant().atZone(zone);
        LocalDate billingDate = billingService.getBillingDateOnOrAfter(
                subscription.getStartDate(), subscription.getBillingIntervalUnit(),
                subscription.getBillingIntervalCount(), userNow.toLocalDate()
        );
        save(subscription, preferences, zone, billingDate);
    }

    @Transactional
    public void advanceAfterDelivery(Subscription subscription, LocalDate deliveredBillingDate) {
        if (!isEnabled(subscription)) {
            repository.deleteById(subscription.getId());
            return;
        }
        UserPreferences preferences = subscription.getUser().getPreferences();
        try {
            ZoneId zone = ZoneId.of(preferences.getTimezone() == null ? "UTC" : preferences.getTimezone());
            LocalDate nextBillingDate = billingService.getBillingDateOnOrAfter(
                    subscription.getStartDate(), subscription.getBillingIntervalUnit(),
                    subscription.getBillingIntervalCount(), deliveredBillingDate.plusDays(1)
            );
            save(subscription, preferences, zone, nextBillingDate);
        } catch (DateTimeException invalidTimezone) {
            repository.deleteById(subscription.getId());
        }
    }

    private void save(Subscription subscription, UserPreferences preferences,
                      ZoneId zone, LocalDate billingDate) {
        LocalTime reminderTime = preferences.getReminderTime() == null
                ? LocalTime.of(9, 0) : preferences.getReminderTime();
        Instant dueAt = billingDate.minusDays(preferences.getReminderDaysBefore())
                .atTime(reminderTime).atZone(zone).toInstant();
        SubscriptionReminderSchedule schedule = repository.findById(subscription.getId())
                .orElseGet(SubscriptionReminderSchedule::new);
        schedule.setSubscription(subscription);
        schedule.setBillingDate(billingDate);
        schedule.setDueAt(dueAt);
        schedule.setUpdatedAt(clock.instant());
        repository.save(schedule);
    }

    private boolean isEnabled(Subscription subscription) {
        UserPreferences preferences = subscription.getUser().getPreferences();
        return subscription.getId() != null
                && subscription.getType() == SubscriptionType.RECURRING
                && Boolean.TRUE.equals(subscription.isEmailNotificationsEnabled())
                && preferences != null
                && Boolean.TRUE.equals(preferences.getEmailNotificationsEnabled());
    }
}
