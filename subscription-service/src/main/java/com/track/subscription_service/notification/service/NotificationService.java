package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.notification.entity.NotificationDelivery;
import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import com.track.subscription_service.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final String RENEWAL_REMINDER = "RENEWAL_REMINDER";
    static final int MAX_ATTEMPTS = 5;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMinutes(5);

    private final EmailService emailService;
    private final TemplateService templateService;
    private final NotificationDeliveryRepository deliveryRepository;
    private final Clock clock;
    private final EmailSuppressionService suppressionService;
    private final UnsubscribeService unsubscribeService;

    public NotificationService(EmailService emailService, TemplateService templateService,
                               NotificationDeliveryRepository deliveryRepository, Clock clock,
                               EmailSuppressionService suppressionService,
                               UnsubscribeService unsubscribeService) {
        this.emailService = emailService;
        this.templateService = templateService;
        this.deliveryRepository = deliveryRepository;
        this.clock = clock;
        this.suppressionService = suppressionService;
        this.unsubscribeService = unsubscribeService;
    }

    public boolean sendSubscriptionReminder(User user, Subscription sub, LocalDate nextDate) {
        if (suppressionService.isSuppressed(user.getEmail())) {
            return false;
        }
        String subject = "Upcoming Subscription Payment";
        int created = deliveryRepository.createIfAbsent(
                sub.getId(), nextDate, RENEWAL_REMINDER, clock.instant()
        );
        if (created == 0) {
            return false;
        }
        try {
            send(user, sub, nextDate, subject);
            deliveryRepository.markSent(sub.getId(), nextDate, RENEWAL_REMINDER, clock.instant());
            return true;
        } catch (RuntimeException exception) {
            deliveryRepository.scheduleInitialRetry(
                    sub.getId(), nextDate, RENEWAL_REMINDER,
                    clock.instant().plus(INITIAL_RETRY_DELAY), errorMessage(exception)
            );
            return false;
        }
    }

    public boolean retry(NotificationDelivery delivery) {
        if (suppressionService.isSuppressed(delivery.getSubscription().getUser().getEmail())) {
            deliveryRepository.markDead(delivery.getId(), clock.instant(), "Email recipient is suppressed");
            return false;
        }
        try {
            send(delivery.getSubscription().getUser(), delivery.getSubscription(),
                    delivery.getBillingDate(), "Upcoming Subscription Payment");
            deliveryRepository.markSent(delivery.getId(), clock.instant());
            return true;
        } catch (RuntimeException exception) {
            Instant now = clock.instant();
            if (delivery.getAttempts() >= MAX_ATTEMPTS) {
                deliveryRepository.markDead(delivery.getId(), now, errorMessage(exception));
            } else {
                deliveryRepository.scheduleRetry(
                        delivery.getId(), now.plus(retryDelay(delivery.getAttempts())), errorMessage(exception)
                );
            }
            return false;
        }
    }

    static Duration retryDelay(int attemptNumber) {
        int exponent = Math.max(0, Math.min(attemptNumber - 1, 8));
        return INITIAL_RETRY_DELAY.multipliedBy(1L << exponent);
    }

    private void send(User user, Subscription subscription, LocalDate billingDate, String subject) {
        String formattedDate = billingDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        String unsubscribeUrl = unsubscribeService.createLink(user);
        String template = templateService.loadTemplate(
                subscription.getName(), formattedDate, unsubscribeUrl
        );
        emailService.sendEmail(user.getEmail(), subject, template, unsubscribeUrl);
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage() == null ? "Unknown delivery failure" : exception.getMessage();
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
