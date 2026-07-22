package com.track.subscription_service.notification.repository;

import com.track.subscription_service.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO notification_delivery
                (subscription_id, billing_date, notification_type, status, attempts, created_at, last_attempt_at)
            VALUES (:subscriptionId, :billingDate, :notificationType, 'PENDING', 1, :createdAt, :createdAt)
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
            SET delivery.status = 'SENT', delivery.sentAt = :sentAt, delivery.lastError = null,
                delivery.nextAttemptAt = null, delivery.claimToken = null
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
            SET delivery.status = 'RETRY_SCHEDULED', delivery.nextAttemptAt = :nextAttemptAt,
                delivery.lastError = :error, delivery.claimToken = null
            WHERE delivery.id = :id
            """)
    int scheduleRetry(@Param("id") Long id, @Param("nextAttemptAt") Instant nextAttemptAt,
                      @Param("error") String error);

    @Modifying
    @Transactional
    @Query("""
            UPDATE NotificationDelivery delivery
            SET delivery.status = 'RETRY_SCHEDULED', delivery.nextAttemptAt = :nextAttemptAt,
                delivery.lastError = :error
            WHERE delivery.subscription.id = :subscriptionId
              AND delivery.billingDate = :billingDate
              AND delivery.notificationType = :notificationType
            """)
    int scheduleInitialRetry(@Param("subscriptionId") Long subscriptionId,
                             @Param("billingDate") LocalDate billingDate,
                             @Param("notificationType") String notificationType,
                             @Param("nextAttemptAt") Instant nextAttemptAt,
                             @Param("error") String error);

    @Modifying
    @Transactional
    @Query("""
            UPDATE NotificationDelivery delivery
            SET delivery.status = 'SENT', delivery.sentAt = :sentAt, delivery.lastError = null,
                delivery.nextAttemptAt = null, delivery.claimToken = null
            WHERE delivery.id = :id
            """)
    int markSent(@Param("id") Long id, @Param("sentAt") Instant sentAt);

    @Modifying
    @Transactional
    @Query("""
            UPDATE NotificationDelivery delivery
            SET delivery.status = 'DEAD', delivery.deadAt = :deadAt,
                delivery.lastError = :error, delivery.nextAttemptAt = null, delivery.claimToken = null
            WHERE delivery.id = :id
            """)
    int markDead(@Param("id") Long id, @Param("deadAt") Instant deadAt, @Param("error") String error);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE notification_delivery
            SET status = 'PROCESSING', attempts = attempts + 1,
                last_attempt_at = :claimedAt, claim_token = :claimToken
            WHERE id IN (
                SELECT id FROM notification_delivery
                WHERE (status = 'RETRY_SCHEDULED' AND next_attempt_at <= :claimedAt)
                   OR (status IN ('PENDING', 'PROCESSING') AND last_attempt_at <= :staleBefore)
                ORDER BY COALESCE(next_attempt_at, last_attempt_at, created_at)
                FOR UPDATE SKIP LOCKED
                LIMIT :batchSize
            )
            """, nativeQuery = true)
    int claimRetryBatch(@Param("claimToken") String claimToken,
                        @Param("claimedAt") Instant claimedAt,
                        @Param("staleBefore") Instant staleBefore,
                        @Param("batchSize") int batchSize);

    @EntityGraph(attributePaths = {"subscription", "subscription.user"})
    List<NotificationDelivery> findAllByClaimToken(String claimToken);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE notification_delivery delivery
            SET status = 'DEAD', dead_at = :now, last_error = :reason,
                next_attempt_at = NULL, claim_token = NULL
            FROM subscription subscription
            WHERE delivery.subscription_id = subscription.id
              AND subscription.user_id = :userId
              AND delivery.status IN ('PENDING', 'PROCESSING', 'RETRY_SCHEDULED')
            """, nativeQuery = true)
    int markOpenDeliveriesDeadForUser(@Param("userId") Long userId,
                                     @Param("now") Instant now,
                                     @Param("reason") String reason);
}
