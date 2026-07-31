package com.track.subscription_service.notification.service;

public interface OutboundEmailSender {
    String send(OutboundEmailMessage message);
}
