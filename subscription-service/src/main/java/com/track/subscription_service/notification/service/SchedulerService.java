package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.subscription.service.BillingService;
import com.track.subscription_service.user.entity.UserPreferences;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
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

//        System.out.println("Checking sub: " + sub.getName());
//        LocalDate nextBillingDate = billingService.getNextBillingDate(
//                sub.getStartDate(),
//                sub.getBillingIntervalUnit(),
//                sub.getBillingIntervalCount()
//        );
//        System.out.println("Next billing date: " + nextBillingDate);
//        System.out.println("Today + 3: " + LocalDate.now().plusDays(3));
//        return !nextBillingDate.isAfter(LocalDate.now().plusDays(3));

        UserPreferences preferences = sub.getUser().getPreferences();

        if (preferences == null || !preferences.getEmailNotificationsEnabled()) {
            return false;
        }

        LocalDate nextBillingDate = billingService.getNextBillingDate(
                sub.getStartDate(),
                sub.getBillingIntervalUnit(),
                sub.getBillingIntervalCount()
        );

        if (nextBillingDate == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        int reminderDaysBefore = preferences.getReminderDaysBefore();

        LocalDate reminderDate = nextBillingDate.minusDays(reminderDaysBefore);

        if (!reminderDate.isEqual(today)) {
            return false;
        }

        LocalTime reminderTime = preferences.getReminderTime();

        if (reminderTime == null) {
            reminderTime = LocalTime.of(9, 0);
        }

        LocalTime now = LocalTime.now();

        return now.getHour() == reminderTime.getHour()
                && now.getMinute() >= reminderTime.getMinute()
                && now.getMinute() < reminderTime.getMinute() + 5;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void checkSubscriptions(){
        List<Subscription> subscriptions = subscriptionRepository.findAll();
//        System.out.println("Scheduler running...");
        for (Subscription sub: subscriptions){
            if (isDueSoon(sub)){
                notificationService.sendSubscriptionReminder(sub.getUser(),sub);
            }
        }


    }
}
