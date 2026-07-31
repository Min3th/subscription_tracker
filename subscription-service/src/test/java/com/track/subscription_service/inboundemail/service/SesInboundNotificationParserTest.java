package com.track.subscription_service.inboundemail.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SesInboundNotificationParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SesInboundNotificationParser parser =
            new SesInboundNotificationParser(objectMapper);

    @Test
    void parsesSesReceiptMetadataFromAnSnsEnvelope() throws Exception {
        String receipt = receipt("sub-token@inbound.subtrak.me", "PASS");
        String envelope = objectMapper.writeValueAsString(Map.of(
                "Type", "Notification",
                "Message", receipt));

        var parsed = parser.parse(envelope);

        assertEquals("ses-inbound-1", parsed.providerMessageId());
        assertEquals("sender@example.com", parsed.envelopeSender());
        assertEquals(
                java.util.List.of("sub-token@inbound.subtrak.me"),
                parsed.envelopeRecipients());
        assertEquals("FAIL", parsed.spamVerdict());
        assertEquals("PASS", parsed.virusVerdict());
        assertEquals(Instant.parse("2026-07-28T04:00:00Z"), parsed.receivedAt());
        assertEquals("inbound-bucket", parsed.bucketName());
        assertEquals("incoming/ses-inbound-1", parsed.objectKey());
    }

    @Test
    void rejectsUnknownTypesMissingRecipientsAndNonS3Actions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("{\"notificationType\":\"Bounce\"}"));
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(receipt("", "PASS")));
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(receipt("user@example.com", "FAIL")
                        .replace("\"type\": \"S3\"", "\"type\": \"SNS\"")));
    }

    private String receipt(String recipient, String virusVerdict) {
        String recipients = recipient.isBlank()
                ? "[]"
                : "[\"" + recipient + "\"]";
        return """
                {
                  "notificationType": "Received",
                  "mail": {
                    "timestamp": "2026-07-28T03:59:59Z",
                    "source": "sender@example.com",
                    "messageId": "ses-inbound-1"
                  },
                  "receipt": {
                    "timestamp": "2026-07-28T04:00:00Z",
                    "recipients": %s,
                    "spamVerdict": {"status": "FAIL"},
                    "virusVerdict": {"status": "%s"},
                    "action": {
                      "type": "S3",
                      "bucketName": "inbound-bucket",
                      "objectKey": "incoming/ses-inbound-1"
                    }
                  }
                }
                """.formatted(recipients, virusVerdict);
    }
}
