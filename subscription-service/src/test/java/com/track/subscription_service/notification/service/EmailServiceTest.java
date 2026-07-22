package com.track.subscription_service.notification.service;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.track.subscription_service.notification.config.SendGridProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private SendGrid sendGrid;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        sendGrid = mock(SendGrid.class);
        SendGridProperties properties = new SendGridProperties();
        properties.setFromEmail("reminders@example.com");
        properties.setFromName("Subscription Tracker");
        emailService = new EmailService(sendGrid, properties);
    }

    @Test
    void acceptsEveryTwoHundredStatus() throws IOException {
        when(sendGrid.api(any(Request.class))).thenReturn(response(202, ""));

        assertDoesNotThrow(() -> emailService.sendEmail(
                "user@example.com", "Reminder", "<p>Payment due</p>"
        ));

        ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
        verify(sendGrid).api(request.capture());
        assertEquals("mail/send", request.getValue().getEndpoint());
    }

    @Test
    void clientErrorIsAProviderDeliveryFailure() throws IOException {
        when(sendGrid.api(any(Request.class))).thenReturn(response(400, "recipient details"));

        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class,
                () -> emailService.sendEmail("user@example.com", "Reminder", "html"));

        assertEquals(400, exception.getStatusCode());
        assertFalse(exception.getMessage().contains("recipient details"));
    }

    @Test
    void serverErrorIsAProviderDeliveryFailure() throws IOException {
        when(sendGrid.api(any(Request.class))).thenReturn(response(503, "unavailable"));

        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class,
                () -> emailService.sendEmail("user@example.com", "Reminder", "html"));

        assertEquals(503, exception.getStatusCode());
    }

    @Test
    void transportErrorPreservesItsCauseWithoutLeakingItsMessage() throws IOException {
        IOException cause = new IOException("connection included sensitive diagnostic");
        when(sendGrid.api(any(Request.class))).thenThrow(cause);

        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class,
                () -> emailService.sendEmail("user@example.com", "Reminder", "html"));

        assertNull(exception.getStatusCode());
        assertSame(cause, exception.getCause());
        assertEquals("Email provider request failed", exception.getMessage());
    }

    private static Response response(int status, String body) {
        return new Response(status, body, Map.of());
    }
}
