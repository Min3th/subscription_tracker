package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.entity.NotificationDelivery;
import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationRetryWorkerTest {

    @Test
    void atomicallyClaimsABoundedBatchAndIsolatesUnexpectedFailures() {
        NotificationDeliveryRepository repository = mock(NotificationDeliveryRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationDelivery first = mock(NotificationDelivery.class);
        NotificationDelivery second = mock(NotificationDelivery.class);
        when(first.getId()).thenReturn(1L);
        when(second.getId()).thenReturn(2L);
        when(repository.findAllByClaimToken(anyString())).thenReturn(List.of(first, second));
        when(notificationService.retry(first)).thenThrow(new RuntimeException("unexpected"));
        Instant now = Instant.parse("2026-07-22T03:30:00Z");

        new NotificationRetryWorker(repository, notificationService,
                Clock.fixed(now, ZoneOffset.UTC)).retryDueDeliveries();

        verify(repository).claimRetryBatch(anyString(), eq(now),
                eq(Instant.parse("2026-07-22T03:20:00Z")), eq(100));
        verify(notificationService).retry(first);
        verify(notificationService).retry(second);
    }
}
