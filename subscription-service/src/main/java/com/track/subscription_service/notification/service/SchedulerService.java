package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.entity.SubscriptionReminderSchedule;
import com.track.subscription_service.notification.repository.SubscriptionReminderScheduleRepository;
import java.time.Clock;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
@EnableScheduling
public class SchedulerService {

    private final SubscriptionReminderScheduleRepository scheduleRepository;
    private final NotificationService notificationService;
    private final ReminderScheduleService scheduleService;
    private final Clock clock;

    public SchedulerService(SubscriptionReminderScheduleRepository scheduleRepository,
                            NotificationService notificationService,
                            ReminderScheduleService scheduleService,
                            Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
        this.scheduleService = scheduleService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void checkSubscriptions(){
        for (SubscriptionReminderSchedule schedule
                : scheduleRepository.findTop100ByDueAtLessThanEqualOrderByDueAtAsc(clock.instant())) {
            try {
                notificationService.sendSubscriptionReminder(
                        schedule.getSubscription().getUser(),
                        schedule.getSubscription(),
                        schedule.getBillingDate()
                );
            } finally {
                scheduleService.advanceAfterDelivery(schedule.getSubscription(), schedule.getBillingDate());
            }
        }


    }
}
