package com.track.subscription_service.notification.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.service.BillingService;
import com.track.subscription_service.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final TemplateService templateService;
    private final BillingService billingService;

    public NotificationService(EmailService emailService, TemplateService templateService, BillingService billingService) {
        this.emailService = emailService;
        this.templateService = templateService;
        this.billingService = billingService;
    }

    public void sendSubscriptionReminder(User user, Subscription sub) {
        String subject = "Upcoming Subscription Payment";
        LocalDate nextDate = billingService.getNextBillingDate(
                sub.getStartDate(),
                sub.getBillingIntervalUnit(),
                sub.getBillingIntervalCount()
        );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        String formattedDate = nextDate.format(formatter);
        String template = templateService.loadTemplate(
                sub.getName(),
                formattedDate
        );


        emailService.sendEmail(user.getEmail(), subject, template);
    }
}
