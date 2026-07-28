package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.config.SesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SesOutboundEmailSenderTest {
    private SesV2Client client;
    private SesOutboundEmailSender sender;

    @BeforeEach
    void setUp() {
        client = mock(SesV2Client.class);
        SesProperties properties = new SesProperties();
        properties.setFromEmail("reminders@example.com");
        properties.setFromName("Subtrak");
        properties.setConfigurationSet("subtrak-events");
        sender = new SesOutboundEmailSender(client, properties);
    }

    @Test
    void sendsRawMimeWithOneClickUnsubscribeHeaders() {
        when(client.sendEmail(any(SendEmailRequest.class))).thenReturn(
                SendEmailResponse.builder().messageId("ses-message-id").build());

        String messageId = sender.send(message());

        assertEquals("ses-message-id", messageId);
        ArgumentCaptor<SendEmailRequest> request =
                ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(client).sendEmail(request.capture());
        assertEquals("subtrak-events", request.getValue().configurationSetName());
        assertEquals("user@example.com",
                request.getValue().destination().toAddresses().get(0));
        String raw = request.getValue().content().raw().data().asUtf8String();
        assertTrue(raw.contains("List-Unsubscribe:"));
        assertTrue(raw.contains("List-Unsubscribe-Post: List-Unsubscribe=One-Click"));
        assertTrue(raw.contains("Content-Type: text/html; charset=UTF-8"));
    }

    @Test
    void mapsTemporarySesFailureToRetryableDeliveryFailure() {
        when(client.sendEmail(any(SendEmailRequest.class))).thenThrow(
                SesV2Exception.builder().statusCode(503).message("internal details").build());

        EmailDeliveryException exception = assertThrows(
                EmailDeliveryException.class, () -> sender.send(message()));

        assertTrue(exception.isRetryable());
        assertEquals(503, exception.getStatusCode());
        assertFalse(exception.getMessage().contains("internal details"));
    }

    @Test
    void mapsPermanentSesFailureWithoutLeakingProviderDetails() {
        when(client.sendEmail(any(SendEmailRequest.class))).thenThrow(
                SesV2Exception.builder().statusCode(400).message("identity details").build());

        EmailDeliveryException exception = assertThrows(
                EmailDeliveryException.class, () -> sender.send(message()));

        assertFalse(exception.isRetryable());
        assertEquals(400, exception.getStatusCode());
        assertFalse(exception.getMessage().contains("identity details"));
    }

    @Test
    void mapsSdkTransportFailureToRetryableDeliveryFailure() {
        SdkClientException cause =
                SdkClientException.create("sensitive network details");
        when(client.sendEmail(any(SendEmailRequest.class))).thenThrow(cause);

        EmailDeliveryException exception = assertThrows(
                EmailDeliveryException.class, () -> sender.send(message()));

        assertTrue(exception.isRetryable());
        assertSame(cause, exception.getCause());
        assertEquals("Email provider request failed", exception.getMessage());
    }

    private OutboundEmailMessage message() {
        return new OutboundEmailMessage(
                "user@example.com",
                "Upcoming payment",
                "<p>Payment due</p>",
                "https://api.example.com/notifications/unsubscribe?token=test");
    }
}
