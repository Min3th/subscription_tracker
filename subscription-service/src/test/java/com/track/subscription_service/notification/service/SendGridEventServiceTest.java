package com.track.subscription_service.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class SendGridEventServiceTest {
    @Test
    void verifiesBeforeMappingPermanentDeliveryEventsToSuppressions() {
        SendGridWebhookVerifier verifier = mock(SendGridWebhookVerifier.class);
        EmailSuppressionService suppressions = mock(EmailSuppressionService.class);
        SendGridEventService service = new SendGridEventService(verifier, suppressions, new ObjectMapper());
        String payload = """
                [{"email":"bounce@example.com","event":"bounce"},
                 {"email":"ok@example.com","event":"delivered"},
                 {"email":"spam@example.com","event":"spamreport"}]
                """;

        service.process(payload, "signature", "timestamp");

        verify(verifier).verify(payload, "signature", "timestamp");
        verify(suppressions).suppress("bounce@example.com", "BOUNCE", "SENDGRID");
        verify(suppressions).suppress("spam@example.com", "SPAMREPORT", "SENDGRID");
        verifyNoMoreInteractions(suppressions);
    }
}
