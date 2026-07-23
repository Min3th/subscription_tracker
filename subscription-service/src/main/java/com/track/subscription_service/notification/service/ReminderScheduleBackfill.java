package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduleBackfill {

    private static final int BATCH_SIZE = 500;

    private final SubscriptionRepository subscriptionRepository;
    private final ReminderScheduleService scheduleService;

    public ReminderScheduleBackfill(SubscriptionRepository subscriptionRepository,
                                    ReminderScheduleService scheduleService) {
        this.subscriptionRepository = subscriptionRepository;
        this.scheduleService = scheduleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillExistingSubscriptions() {
        int pageNumber = 0;
        Page<Subscription> page;
        do {
            page = subscriptionRepository.findAllBy(PageRequest.of(pageNumber++, BATCH_SIZE));
            page.forEach(scheduleService::refresh);
        } while (page.hasNext());
    }
}
