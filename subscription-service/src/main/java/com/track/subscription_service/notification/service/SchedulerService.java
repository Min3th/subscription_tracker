package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.subscription.service.BillingService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@EnableScheduling
public class SchedulerService {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;
    private final BillingService billingService;

    public SchedulerService(SubscriptionRepository subscriptionRepository, NotificationService notificationService, BillingService billingService) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationService = notificationService;
        this.billingService = billingService;
    }

    private boolean isDueSoon(Subscription sub) {

        LocalDate nextBillingDate = billingService.getNextBillingDate(
                sub.getStartDate(),
                sub.getBillingIntervalUnit(),
                sub.getBillingIntervalCount()
        );
        return !nextBillingDate.isAfter(LocalDate.now().plusDays(3));
    }

//    @Scheduled(cron = "0 0 9 * * ?") at 9 am
    @Scheduled(cron = "0 */1 * * * ?") // for testing, every 1 min
    public void checkSubscriptions(){
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        System.out.println("Scheduler running...");
        for (Subscription sub: subscriptions){
            if (isDueSoon(sub)){
                notificationService.sendSubscriptionReminder(sub.getUser(),sub);
            }
        }


    }
}
