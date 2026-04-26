package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final EmailService emailService;

    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendSubscriptionReminder(User user, Subscription sub) {
        String subject = "Upcoming Subscription Payment";
        String body = "Your subscription " + sub.getName() + " will renew on " + "THIS DATE";

        emailService.sendEmail(user.getEmail(), subject, body);
    }
}
