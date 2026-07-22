package com.track.subscription_service.notification.service;

import com.sendgrid.helpers.eventwebhook.EventWebhook;
import com.track.subscription_service.notification.config.SendGridProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class SendGridWebhookVerifier {
    private static final Duration SIGNATURE_MAX_AGE = Duration.ofMinutes(5);
    private final SendGridProperties properties;
    private final Clock clock;
    private final EventWebhook eventWebhook = new EventWebhook();

    public SendGridWebhookVerifier(SendGridProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void verify(String payload, String signature, String timestamp) {
        if (signature == null || timestamp == null || properties.getEventWebhookPublicKey() == null
                || properties.getEventWebhookPublicKey().isBlank()) {
            throw new SecurityException("SendGrid webhook verification is not configured");
        }
        try {
            Instant signedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            if (Duration.between(signedAt, clock.instant()).abs().compareTo(SIGNATURE_MAX_AGE) > 0) {
                throw new SecurityException("SendGrid webhook timestamp is outside the allowed window");
            }
            if (!eventWebhook.VerifySignature(
                    eventWebhook.ConvertPublicKeyToECDSA(properties.getEventWebhookPublicKey()),
                    payload, signature, timestamp)) {
                throw new SecurityException("Invalid SendGrid webhook signature");
            }
        } catch (SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SecurityException("Invalid SendGrid webhook signature", exception);
        }
    }
}
