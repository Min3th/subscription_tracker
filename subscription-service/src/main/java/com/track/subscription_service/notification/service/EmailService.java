package com.track.subscription_service.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final OutboundEmailSender sender;

    public EmailService(OutboundEmailSender sender) {
        this.sender = sender;
    }

    public void sendEmail(String to, String subject, String html, String unsubscribeUrl) {

        String providerMessageId = sender.send(
                new OutboundEmailMessage(to, subject, html, unsubscribeUrl));
        log.debug("Email provider accepted message {}", providerMessageId);
    }
}
