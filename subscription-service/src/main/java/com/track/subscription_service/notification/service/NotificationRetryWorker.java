package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.entity.NotificationDelivery;
import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationRetryWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryWorker.class);
    private static final int BATCH_SIZE = 100;
    private static final Duration STALE_CLAIM_AGE = Duration.ofMinutes(10);

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public NotificationRetryWorker(NotificationDeliveryRepository deliveryRepository,
                                   NotificationService notificationService, Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void retryDueDeliveries() {
        Instant now = clock.instant();
        String claimToken = UUID.randomUUID().toString();
        deliveryRepository.claimRetryBatch(claimToken, now, now.minus(STALE_CLAIM_AGE), BATCH_SIZE);

        for (NotificationDelivery delivery : deliveryRepository.findAllByClaimToken(claimToken)) {
            try {
                notificationService.retry(delivery);
            } catch (RuntimeException exception) {
                log.error("Unexpected failure while processing notification delivery {}", delivery.getId(), exception);
            }
        }
    }
}
