package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.Duration;
import com.track.subscription_service.notification.entity.NotificationDelivery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private EmailService emailService;
    private TemplateService templateService;
    private NotificationDeliveryRepository deliveryRepository;
    private NotificationService service;
    private Subscription subscription;
    private User user;
    private LocalDate billingDate;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        templateService = mock(TemplateService.class);
        deliveryRepository = mock(NotificationDeliveryRepository.class);
        service = new NotificationService(emailService, templateService, deliveryRepository,
                Clock.fixed(Instant.parse("2026-07-22T03:30:00Z"), ZoneOffset.UTC));

        subscription = new Subscription();
        subscription.setId(42L);
        subscription.setName("Example");
        subscription.setStartDate(LocalDate.of(2026, 1, 1));
        subscription.setBillingIntervalCount(1);

        user = new User();
        user.setEmail("user@example.com");
        billingDate = LocalDate.of(2026, 8, 1);
    }

    @Test
    void duplicateIdempotencyKeyDoesNotSendAgain() {
        when(deliveryRepository.createIfAbsent(eq(42L), eq(billingDate), eq("RENEWAL_REMINDER"), any(Instant.class)))
                .thenReturn(0);

        assertFalse(service.sendSubscriptionReminder(user, subscription, billingDate));

        verifyNoInteractions(emailService, templateService);
        verify(deliveryRepository, never()).markSent(anyLong(), any(), anyString(), any());
    }

    @Test
    void firstDeliveryIsRecordedAndMarkedSent() {
        when(deliveryRepository.createIfAbsent(eq(42L), eq(billingDate), eq("RENEWAL_REMINDER"), any(Instant.class)))
                .thenReturn(1);
        when(templateService.loadTemplate(eq("Example"), anyString())).thenReturn("<p>Reminder</p>");

        assertTrue(service.sendSubscriptionReminder(user, subscription, billingDate));

        verify(emailService).sendEmail("user@example.com", "Upcoming Subscription Payment", "<p>Reminder</p>");
        verify(deliveryRepository).markSent(eq(42L), eq(billingDate), eq("RENEWAL_REMINDER"), any(Instant.class));
    }

    @Test
    void initialFailureIsScheduledForRetryWithoutEscaping() {
        when(deliveryRepository.createIfAbsent(anyLong(), any(), anyString(), any())).thenReturn(1);
        when(templateService.loadTemplate(anyString(), anyString())).thenReturn("template");
        doThrow(new RuntimeException("temporary failure")).when(emailService)
                .sendEmail(anyString(), anyString(), anyString());

        assertFalse(service.sendSubscriptionReminder(user, subscription, billingDate));

        verify(deliveryRepository).scheduleInitialRetry(eq(42L), eq(billingDate), eq("RENEWAL_REMINDER"),
                eq(Instant.parse("2026-07-22T03:35:00Z")), eq("temporary failure"));
    }

    @Test
    void retryFailureUsesExponentialBackoff() {
        NotificationDelivery delivery = mock(NotificationDelivery.class);
        when(delivery.getId()).thenReturn(9L);
        when(delivery.getSubscription()).thenReturn(subscription);
        when(delivery.getBillingDate()).thenReturn(billingDate);
        when(delivery.getAttempts()).thenReturn(3);
        subscription.setUser(user);
        when(templateService.loadTemplate(anyString(), anyString())).thenReturn("template");
        doThrow(new RuntimeException("still unavailable")).when(emailService)
                .sendEmail(anyString(), anyString(), anyString());

        assertFalse(service.retry(delivery));

        verify(deliveryRepository).scheduleRetry(9L, Instant.parse("2026-07-22T03:50:00Z"),
                "still unavailable");
    }

    @Test
    void finalRetryFailureMovesDeliveryToDeadLetterState() {
        NotificationDelivery delivery = mock(NotificationDelivery.class);
        when(delivery.getId()).thenReturn(9L);
        when(delivery.getSubscription()).thenReturn(subscription);
        when(delivery.getBillingDate()).thenReturn(billingDate);
        when(delivery.getAttempts()).thenReturn(NotificationService.MAX_ATTEMPTS);
        subscription.setUser(user);
        when(templateService.loadTemplate(anyString(), anyString())).thenReturn("template");
        doThrow(new RuntimeException("permanent failure")).when(emailService)
                .sendEmail(anyString(), anyString(), anyString());

        assertFalse(service.retry(delivery));

        verify(deliveryRepository).markDead(9L, Instant.parse("2026-07-22T03:30:00Z"), "permanent failure");
        verify(deliveryRepository, never()).scheduleRetry(anyLong(), any(), anyString());
    }

    @Test
    void retryDelayIsCappedToAvoidOverflow() {
        assertTrue(NotificationService.retryDelay(100).compareTo(Duration.ofHours(24)) < 0);
    }
}
