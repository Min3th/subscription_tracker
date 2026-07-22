package com.track.subscription_service.notification.repository;

import com.track.subscription_service.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO notification_delivery
                (subscription_id, billing_date, notification_type, status, attempts, created_at)
            VALUES (:subscriptionId, :billingDate, :notificationType, 'PENDING', 1, :createdAt)
            ON CONFLICT ON CONSTRAINT uk_notification_delivery_idempotency DO NOTHING
            """, nativeQuery = true)
    int createIfAbsent(@Param("subscriptionId") Long subscriptionId,
                       @Param("billingDate") LocalDate billingDate,
                       @Param("notificationType") String notificationType,
                       @Param("createdAt") Instant createdAt);

    @Modifying
    @Transactional
    @Query("""
            UPDATE NotificationDelivery delivery
            SET delivery.status = 'SENT', delivery.sentAt = :sentAt, delivery.lastError = null
            WHERE delivery.subscription.id = :subscriptionId
              AND delivery.billingDate = :billingDate
              AND delivery.notificationType = :notificationType
            """)
    int markSent(@Param("subscriptionId") Long subscriptionId,
                 @Param("billingDate") LocalDate billingDate,
                 @Param("notificationType") String notificationType,
                 @Param("sentAt") Instant sentAt);

    @Modifying
    @Transactional
    @Query("""
            UPDATE NotificationDelivery delivery
            SET delivery.status = 'FAILED', delivery.lastError = :error
            WHERE delivery.subscription.id = :subscriptionId
              AND delivery.billingDate = :billingDate
              AND delivery.notificationType = :notificationType
            """)
    int markFailed(@Param("subscriptionId") Long subscriptionId,
                   @Param("billingDate") LocalDate billingDate,
                   @Param("notificationType") String notificationType,
                   @Param("error") String error);
}
