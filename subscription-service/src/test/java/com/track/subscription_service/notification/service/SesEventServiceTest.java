package com.track.subscription_service.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SesEventServiceTest {
    private EmailSuppressionService suppressions;
    private SesEventService service;

    @BeforeEach
    void setUp() {
        suppressions = mock(EmailSuppressionService.class);
        service = new SesEventService(suppressions, new ObjectMapper());
    }

    @Test
    void permanentBounceSuppressesEveryRecipientFromAnSnsEnvelope() {
        service.process(snsEnvelope("""
                {
                  "eventType": "Bounce",
                  "mail": {"messageId": "ses-1"},
                  "bounce": {
                    "bounceType": "Permanent",
                    "bouncedRecipients": [
                      {"emailAddress": "first@example.com"},
                      {"emailAddress": "second@example.com"}
                    ]
                  }
                }
                """));

        verify(suppressions).suppress("first@example.com", "BOUNCE", "SES");
        verify(suppressions).suppress("second@example.com", "BOUNCE", "SES");
    }

    @Test
    void complaintSuppressesRecipient() {
        service.process("""
                {
                  "notificationType": "Complaint",
                  "complaint": {
                    "complainedRecipients": [
                      {"emailAddress": "complaint@example.com"}
                    ]
                  }
                }
                """);

        verify(suppressions).suppress("complaint@example.com", "COMPLAINT", "SES");
    }

    @Test
    void transientBounceDoesNotSuppress() {
        service.process("""
                {
                  "eventType": "Bounce",
                  "mail": {"messageId": "ses-2"},
                  "bounce": {
                    "bounceType": "Transient",
                    "bouncedRecipients": [{"emailAddress": "later@example.com"}]
                  }
                }
                """);

        verifyNoInteractions(suppressions);
    }

    @Test
    void deliveryDoesNotSuppress() {
        service.process("""
                {"eventType": "Delivery", "mail": {"messageId": "ses-3"}}
                """);

        verifyNoInteractions(suppressions);
    }

    @Test
    void duplicatePermanentBounceRemainsSafeThroughSuppressionUpsert() {
        String event = """
                {
                  "eventType": "Bounce",
                  "bounce": {
                    "bounceType": "Permanent",
                    "bouncedRecipients": [{"emailAddress": "repeat@example.com"}]
                  }
                }
                """;

        service.process(event);
        service.process(event);

        verify(suppressions, org.mockito.Mockito.times(2))
                .suppress("repeat@example.com", "BOUNCE", "SES");
    }

    @Test
    void unknownAndMalformedEventsFailForQueueRedrive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.process("{\"eventType\": \"Rendering Failure\"}"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.process(
                        "{\"eventType\": \"Bounce\", \"bounce\": {}}"));
        assertThrows(IllegalArgumentException.class, () -> service.process("not-json"));
        verifyNoInteractions(suppressions);
    }

    private String snsEnvelope(String message) {
        try {
            return new ObjectMapper().writeValueAsString(
                    java.util.Map.of("Type", "Notification", "Message", message));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
