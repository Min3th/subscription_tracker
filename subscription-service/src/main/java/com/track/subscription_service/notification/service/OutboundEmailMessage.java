package com.track.subscription_service.notification.service;

public record OutboundEmailMessage(
        String to,
        String subject,
        String html,
        String unsubscribeUrl
) {
}
