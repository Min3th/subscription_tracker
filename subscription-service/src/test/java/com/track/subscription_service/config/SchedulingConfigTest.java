package com.track.subscription_service.config;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import com.track.subscription_service.inboundemail.repository.SubscriptionSuggestionRepository;
import com.track.subscription_service.inboundemail.service.InboundEmailProcessingWorker;
import com.track.subscription_service.inboundemail.service.InboundEmailProcessor;
import com.track.subscription_service.inboundemail.service.InboundEmailRetentionWorker;
import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import com.track.subscription_service.notification.repository.SubscriptionReminderScheduleRepository;
import com.track.subscription_service.notification.service.*;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SchedulingConfigTest {
    private ApplicationContextRunner context() {
        return new ApplicationContextRunner()
                .withBean(Clock.class, Clock::systemUTC)
                .withBean(InboundEmailProperties.class, InboundEmailProperties::new)
                .withBean(InboundEmailRepository.class, () -> mock(InboundEmailRepository.class))
                .withBean(SubscriptionSuggestionRepository.class, () -> mock(SubscriptionSuggestionRepository.class))
                .withBean(InboundEmailProcessor.class, () -> mock(InboundEmailProcessor.class))
                .withBean(NotificationDeliveryRepository.class, () -> mock(NotificationDeliveryRepository.class))
                .withBean(SubscriptionReminderScheduleRepository.class, () -> mock(SubscriptionReminderScheduleRepository.class))
                .withBean(NotificationService.class, () -> mock(NotificationService.class))
                .withBean(ReminderScheduleService.class, () -> mock(ReminderScheduleService.class))
                .withPropertyValues("app.notification-retry.initial-delay-ms=3600000",
                        "app.inbound-email.processing-initial-delay-ms=3600000")
                .withUserConfiguration(SchedulingConfig.class, SchedulerService.class,
                        NotificationRetryWorker.class, InboundEmailProcessingWorker.class,
                        InboundEmailRetentionWorker.class);
    }

    @Test
    void masterSwitchRemovesSchedulerInfrastructure() {
        context().withPropertyValues("app.scheduling.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(context).hasSingleBean(InboundEmailProcessingWorker.class);
        });
    }

    @Test
    void individualFlagsDisableDatabaseWorkers() {
        context().withPropertyValues("app.reminders.enabled=false", "app.notification-retry.enabled=false",
                "app.inbound-email.processing-enabled=false", "app.inbound-email.retention-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SchedulerService.class)
                            .doesNotHaveBean(NotificationRetryWorker.class)
                            .doesNotHaveBean(InboundEmailProcessingWorker.class)
                            .doesNotHaveBean(InboundEmailRetentionWorker.class);
                    assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).isEmpty();
                });
    }

    @Test
    void disablingRemindersDoesNotDisableOtherSchedules() {
        context().withPropertyValues("app.reminders.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).hasSize(3);
        });
    }

    @Test
    void configuredIntervalsReachActualRegisteredTasks() {
        context().withPropertyValues("app.reminders.cron=0 */15 * * * *",
                "app.notification-retry.delay-ms=900000", "app.inbound-email.processing-delay-ms=600000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
                    assertThat(tasks).hasSize(4);
                    assertThat(tasks.stream().map(t -> t.getTask()).filter(CronTask.class::isInstance)
                            .map(t -> ((CronTask) t).getExpression())).contains("0 */15 * * * *");
                    assertThat(tasks.stream().map(t -> t.getTask()).filter(FixedDelayTask.class::isInstance)
                            .map(t -> ((FixedDelayTask) t).getIntervalDuration().toMillis()))
                            .containsExactlyInAnyOrder(900000L, 600000L);
                });
    }
}
