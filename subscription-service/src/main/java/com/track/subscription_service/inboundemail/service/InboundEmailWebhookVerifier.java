package com.track.subscription_service.inboundemail.service;

import com.sendgrid.helpers.eventwebhook.EventWebhook;
import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class InboundEmailWebhookVerifier {
    private static final Duration SIGNATURE_MAX_AGE = Duration.ofMinutes(5);

    private final InboundEmailProperties properties;
    private final Clock clock;
    private final EventWebhook eventWebhook;

    @Autowired
    public InboundEmailWebhookVerifier(InboundEmailProperties properties, Clock clock) {
        this(properties, clock, new EventWebhook());
    }

    InboundEmailWebhookVerifier(InboundEmailProperties properties, Clock clock,
                                EventWebhook eventWebhook) {
        this.properties = properties;
        this.clock = clock;
        this.eventWebhook = eventWebhook;
    }

    public void verify(byte[] rawBody, String signature, String timestamp) {
        if (rawBody == null || signature == null || timestamp == null
                || properties.getWebhookPublicKey() == null
                || properties.getWebhookPublicKey().isBlank()) {
            throw new SecurityException("Inbound email webhook verification is not configured");
        }
        try {
            Instant signedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            if (Duration.between(signedAt, clock.instant()).abs().compareTo(SIGNATURE_MAX_AGE) > 0) {
                throw new SecurityException("Inbound email webhook timestamp is outside the allowed window");
            }
            boolean valid = eventWebhook.VerifySignature(
                    eventWebhook.ConvertPublicKeyToECDSA(properties.getWebhookPublicKey()),
                    rawBody,
                    signature,
                    timestamp
            );
            if (!valid) {
                throw new SecurityException("Invalid inbound email webhook signature");
            }
        } catch (SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SecurityException("Invalid inbound email webhook signature", exception);
        }
    }
}
