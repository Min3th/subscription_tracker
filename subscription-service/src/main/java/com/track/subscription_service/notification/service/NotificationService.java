package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import com.track.subscription_service.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final String RENEWAL_REMINDER = "RENEWAL_REMINDER";

    private final EmailService emailService;
    private final TemplateService templateService;
    private final NotificationDeliveryRepository deliveryRepository;

    public NotificationService(EmailService emailService, TemplateService templateService,
                               NotificationDeliveryRepository deliveryRepository) {
        this.emailService = emailService;
        this.templateService = templateService;
        this.deliveryRepository = deliveryRepository;
    }

    public boolean sendSubscriptionReminder(User user, Subscription sub, LocalDate nextDate) {
        String subject = "Upcoming Subscription Payment";
        int created = deliveryRepository.createIfAbsent(
                sub.getId(), nextDate, RENEWAL_REMINDER, Instant.now()
        );
        if (created == 0) {
            return false;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        String formattedDate = nextDate.format(formatter);
        String template = templateService.loadTemplate(
                sub.getName(),
                formattedDate
        );


        try {
            emailService.sendEmail(user.getEmail(), subject, template);
            deliveryRepository.markSent(sub.getId(), nextDate, RENEWAL_REMINDER, Instant.now());
            return true;
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null ? "Unknown delivery failure" : exception.getMessage();
            deliveryRepository.markFailed(
                    sub.getId(), nextDate, RENEWAL_REMINDER,
                    message.substring(0, Math.min(message.length(), 1000))
            );
            throw exception;
        }
    }
}
