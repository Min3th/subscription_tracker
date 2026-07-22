package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.service.BillingService;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private EmailService emailService;
    private TemplateService templateService;
    private BillingService billingService;
    private NotificationDeliveryRepository deliveryRepository;
    private NotificationService service;
    private Subscription subscription;
    private User user;
    private LocalDate billingDate;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        templateService = mock(TemplateService.class);
        billingService = mock(BillingService.class);
        deliveryRepository = mock(NotificationDeliveryRepository.class);
        service = new NotificationService(emailService, templateService, billingService, deliveryRepository);

        subscription = new Subscription();
        subscription.setId(42L);
        subscription.setName("Example");
        subscription.setStartDate(LocalDate.of(2026, 1, 1));
        subscription.setBillingIntervalUnit("month");
        subscription.setBillingIntervalCount(1);

        user = new User();
        user.setEmail("user@example.com");
        billingDate = LocalDate.of(2026, 8, 1);
        when(billingService.getNextBillingDate(any(), anyString(), anyInt())).thenReturn(billingDate);
    }

    @Test
    void duplicateIdempotencyKeyDoesNotSendAgain() {
        when(deliveryRepository.createIfAbsent(eq(42L), eq(billingDate), eq("RENEWAL_REMINDER"), any(Instant.class)))
                .thenReturn(0);

        assertFalse(service.sendSubscriptionReminder(user, subscription));

        verifyNoInteractions(emailService, templateService);
        verify(deliveryRepository, never()).markSent(anyLong(), any(), anyString(), any());
    }

    @Test
    void firstDeliveryIsRecordedAndMarkedSent() {
        when(deliveryRepository.createIfAbsent(eq(42L), eq(billingDate), eq("RENEWAL_REMINDER"), any(Instant.class)))
                .thenReturn(1);
        when(templateService.loadTemplate(eq("Example"), anyString())).thenReturn("<p>Reminder</p>");

        assertTrue(service.sendSubscriptionReminder(user, subscription));

        verify(emailService).sendEmail("user@example.com", "Upcoming Subscription Payment", "<p>Reminder</p>");
        verify(deliveryRepository).markSent(eq(42L), eq(billingDate), eq("RENEWAL_REMINDER"), any(Instant.class));
    }
}
