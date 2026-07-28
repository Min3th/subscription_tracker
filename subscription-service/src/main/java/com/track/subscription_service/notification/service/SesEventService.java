package com.track.subscription_service.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

@Service
public class SesEventService {
    private static final Logger log = LoggerFactory.getLogger(SesEventService.class);

    private final EmailSuppressionService suppressionService;
    private final ObjectMapper objectMapper;

    public SesEventService(
            EmailSuppressionService suppressionService,
            ObjectMapper objectMapper) {
        this.suppressionService = suppressionService;
        this.objectMapper = objectMapper;
    }

    public void process(String sqsBody) {
        try {
            JsonNode envelope = objectMapper.readTree(sqsBody);
            JsonNode event = unwrapSnsEnvelope(envelope);
            String eventType = eventType(event);
            switch (eventType) {
                case "bounce" -> processBounce(event);
                case "complaint" -> processComplaint(event);
                case "delivery" -> logDelivery(event);
                default -> throw new IllegalArgumentException(
                        "Unknown SES event type: " + eventType);
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid SES event payload", exception);
        }
    }

    private JsonNode unwrapSnsEnvelope(JsonNode envelope) {
        if (envelope == null || !envelope.isObject()) {
            throw new IllegalArgumentException("SES event payload must be an object");
        }
        JsonNode message = envelope.get("Message");
        if (message == null) {
            return envelope;
        }
        if (!message.isTextual() || message.asText().isBlank()) {
            throw new IllegalArgumentException("SNS Message must contain an SES event");
        }
        return objectMapper.readTree(message.asText());
    }

    private String eventType(JsonNode event) {
        String eventType = event.path("eventType").asText("");
        if (eventType.isBlank()) {
            eventType = event.path("notificationType").asText("");
        }
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("SES event type is required");
        }
        return eventType.toLowerCase(Locale.ROOT);
    }

    private void processBounce(JsonNode event) {
        JsonNode bounce = event.path("bounce");
        String bounceType = bounce.path("bounceType").asText("");
        JsonNode recipients = bounce.path("bouncedRecipients");
        validateRecipients(recipients, "bounce");
        if (!"permanent".equalsIgnoreCase(bounceType)
                && !"transient".equalsIgnoreCase(bounceType)
                && !"undetermined".equalsIgnoreCase(bounceType)) {
            throw new IllegalArgumentException("SES bounce type is invalid");
        }
        if (!"permanent".equalsIgnoreCase(bounceType)) {
            log.info(
                    "Ignoring non-permanent SES bounce for message {} with type {}",
                    providerMessageId(event),
                    bounceType);
            return;
        }
        suppressRecipients(recipients, "BOUNCE");
    }

    private void processComplaint(JsonNode event) {
        suppressRecipients(
                event.path("complaint").path("complainedRecipients"),
                "COMPLAINT");
    }

    private void suppressRecipients(JsonNode recipients, String reason) {
        validateRecipients(recipients, reason.toLowerCase(Locale.ROOT));
        for (JsonNode recipient : recipients) {
            String email = recipient.path("emailAddress").asText("");
            suppressionService.suppress(email, reason, "SES");
        }
    }

    private void validateRecipients(JsonNode recipients, String eventName) {
        if (!recipients.isArray() || recipients.isEmpty()) {
            throw new IllegalArgumentException(
                    "SES " + eventName + " recipients are required");
        }
        for (JsonNode recipient : recipients) {
            if (recipient.path("emailAddress").asText("").isBlank()) {
                throw new IllegalArgumentException("SES event recipient email is required");
            }
        }
    }

    private void logDelivery(JsonNode event) {
        log.info("SES delivery confirmed for message {}", providerMessageId(event));
    }

    private String providerMessageId(JsonNode event) {
        return event.path("mail").path("messageId").asText("unknown");
    }
}
