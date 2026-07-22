package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableScheduling
public class SchedulerService {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;
    private final ReminderEligibilityService eligibilityService;

    public SchedulerService(SubscriptionRepository subscriptionRepository,
                            NotificationService notificationService,
                            ReminderEligibilityService eligibilityService) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationService = notificationService;
        this.eligibilityService = eligibilityService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void checkSubscriptions(){
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        for (Subscription sub: subscriptions){
            eligibilityService.dueBillingDate(sub).ifPresent(
                    billingDate -> notificationService.sendSubscriptionReminder(sub.getUser(), sub, billingDate)
            );
        }


    }
}
