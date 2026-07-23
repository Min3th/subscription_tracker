package com.track.subscription_service.notification.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class SendGridEventService {
    private static final Set<String> SUPPRESSION_EVENTS = Set.of(
            "bounce", "dropped", "spamreport", "unsubscribe", "group_unsubscribe"
    );

    private final SendGridWebhookVerifier webhookVerifier;
    private final EmailSuppressionService suppressionService;
    private final ObjectMapper objectMapper;

    public SendGridEventService(SendGridWebhookVerifier webhookVerifier,
                                EmailSuppressionService suppressionService,
                                ObjectMapper objectMapper) {
        this.webhookVerifier = webhookVerifier;
        this.suppressionService = suppressionService;
        this.objectMapper = objectMapper;
    }

    public void process(String payload, String signature, String timestamp) {
        webhookVerifier.verify(payload, signature, timestamp);
        try {
            JsonNode events = objectMapper.readTree(payload);
            if (!events.isArray()) {
                throw new IllegalArgumentException("SendGrid event payload must be an array");
            }
            for (JsonNode event : events) {
                String eventName = event.path("event").asText("").toLowerCase(Locale.ROOT);
                String email = event.path("email").asText("");
                if (SUPPRESSION_EVENTS.contains(eventName) && !email.isBlank()) {
                    suppressionService.suppress(email, eventName.toUpperCase(Locale.ROOT), "SENDGRID");
                }
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid SendGrid event payload", exception);
        }
    }

}
