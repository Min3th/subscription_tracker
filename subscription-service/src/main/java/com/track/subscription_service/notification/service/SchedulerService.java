package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;

public class SchedulerService {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    private boolean isDueSoon(Subscription sub) {

        LocalDate nextBillingDate = getNextBillingDate(
                sub.getStartDate(),
                sub.getBillingIntervalUnit(),
                sub.getBillingIntervalCount()
        );
        return sub.getNextBillingDate().isBefore(LocalDate.now().plusDays(3));
    }

    @Scheduled(cron = "0 0 9 * *")
    public void checkSubscriptions(){
        List<Subscription> subscriptions = subscriptionRepository.findAll();

        for (Subscription sub: subscriptions){
            if (isDueSoon(sub)){
                notificationService.sendSubscriptionReminder(sub.getUser(),sub);
            }
        }


    }
}
