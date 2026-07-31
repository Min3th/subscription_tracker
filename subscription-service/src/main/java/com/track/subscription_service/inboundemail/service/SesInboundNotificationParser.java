package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.SesInboundObject;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SesInboundNotificationParser {
    private final ObjectMapper objectMapper;

    public SesInboundNotificationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SesInboundObject parse(String sqsBody) {
        try {
            JsonNode envelope = objectMapper.readTree(sqsBody);
            JsonNode notification = unwrapSnsEnvelope(envelope);
            String notificationType =
                    notification.path("notificationType").asText("");
            if (!"received".equalsIgnoreCase(notificationType)) {
                throw new IllegalArgumentException(
                        "Unknown SES inbound notification type: " + notificationType);
            }

            JsonNode mail = notification.path("mail");
            JsonNode receipt = notification.path("receipt");
            JsonNode action = receipt.path("action");
            if (!"s3".equalsIgnoreCase(action.path("type").asText(""))) {
                throw new IllegalArgumentException(
                        "SES inbound notification does not reference an S3 action");
            }

            return new SesInboundObject(
                    required(mail, "messageId"),
                    required(mail, "source"),
                    recipients(receipt.path("recipients")),
                    required(receipt.path("spamVerdict"), "status"),
                    required(receipt.path("virusVerdict"), "status"),
                    timestamp(receipt.path("timestamp").asText(
                            mail.path("timestamp").asText(""))),
                    required(action, "bucketName"),
                    required(action, "objectKey"));
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid SES inbound notification payload", exception);
        }
    }

    private JsonNode unwrapSnsEnvelope(JsonNode envelope) {
        if (envelope == null || !envelope.isObject()) {
            throw new IllegalArgumentException(
                    "SES inbound notification must be an object");
        }
        JsonNode message = envelope.get("Message");
        if (message == null) {
            return envelope;
        }
        if (!message.isTextual() || message.asText().isBlank()) {
            throw new IllegalArgumentException(
                    "SNS Message must contain an SES receipt notification");
        }
        return objectMapper.readTree(message.asText());
    }

    private List<String> recipients(JsonNode value) {
        if (!value.isArray() || value.isEmpty()) {
            throw new IllegalArgumentException(
                    "SES receipt envelope recipients are required");
        }
        List<String> recipients = new ArrayList<>();
        for (JsonNode recipient : value) {
            if (!recipient.isTextual() || recipient.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "SES receipt envelope recipient is invalid");
            }
            recipients.add(recipient.asText());
        }
        return recipients;
    }

    private String required(JsonNode parent, String field) {
        String value = parent.path(field).asText("");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "SES inbound " + field + " is required");
        }
        return value;
    }

    private Instant timestamp(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "SES inbound receipt timestamp is required");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "SES inbound receipt timestamp is invalid", exception);
        }
    }
}
