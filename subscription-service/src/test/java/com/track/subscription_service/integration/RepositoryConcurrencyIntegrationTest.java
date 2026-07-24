package com.track.subscription_service.integration;

import com.track.subscription_service.notification.entity.NotificationDelivery;
import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "google.client.id=test-google-client-id",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.sendgrid.eventWebhookPublicKey="
})
class RepositoryConcurrencyIntegrationTest extends PostgresIntegrationTest {

    private static final String TEST_USER_PREFIX = "repository-integration-";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SubscriptionRepository subscriptions;

    @Autowired
    private NotificationDeliveryRepository deliveries;

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id LIKE ?", TEST_USER_PREFIX + "%");
    }

    @Test
    void subscriptionQueriesAreIsolatedByGoogleId() {
        long firstUserId = createUser("tenant-a");
        long secondUserId = createUser("tenant-b");
        long firstSubscriptionId = createSubscription(firstUserId, "Tenant A subscription");
        long secondSubscriptionId = createSubscription(secondUserId, "Tenant B subscription");

        List<Subscription> firstTenantSubscriptions =
                subscriptions.findByUser_GoogleId(TEST_USER_PREFIX + "tenant-a");

        assertEquals(1, firstTenantSubscriptions.size());
        assertEquals(firstSubscriptionId, firstTenantSubscriptions.get(0).getId());
        assertTrue(subscriptions.findByIdAndUser_GoogleId(
                firstSubscriptionId, TEST_USER_PREFIX + "tenant-a").isPresent());
        assertTrue(subscriptions.findByIdAndUser_GoogleId(
                secondSubscriptionId, TEST_USER_PREFIX + "tenant-b").isPresent());
        assertFalse(subscriptions.findByIdAndUser_GoogleId(
                secondSubscriptionId, TEST_USER_PREFIX + "tenant-a").isPresent());
        assertFalse(subscriptions.findByIdAndUser_GoogleId(
                firstSubscriptionId, TEST_USER_PREFIX + "tenant-b").isPresent());
    }

    @Test
    void idempotencyKeyPreventsDuplicateNotificationDeliveries() {
        long userId = createUser("idempotency");
        long subscriptionId = createSubscription(userId, "Idempotency subscription");
        LocalDate billingDate = LocalDate.of(2026, 8, 1);
        Instant createdAt = Instant.parse("2026-07-24T08:00:00Z");

        assertEquals(1, deliveries.createIfAbsent(
                subscriptionId, billingDate, "UPCOMING_PAYMENT", createdAt));
        assertEquals(0, deliveries.createIfAbsent(
                subscriptionId, billingDate, "UPCOMING_PAYMENT", createdAt.plusSeconds(1)));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_delivery WHERE subscription_id = ?",
                Integer.class,
                subscriptionId));
    }

    @Test
    void competingWorkersClaimDisjointRetryBatches() throws Exception {
        long userId = createUser("claim");
        long subscriptionId = createSubscription(userId, "Claim subscription");
        Instant claimedAt = Instant.parse("2026-07-24T08:00:00Z");
        Instant staleBefore = claimedAt.minusSeconds(300);
        seedDueDeliveries(subscriptionId, claimedAt, 20);

        String firstToken = "11111111-1111-1111-1111-111111111111";
        String secondToken = "22222222-2222-2222-2222-222222222222";
        CountDownLatch workersReady = new CountDownLatch(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> firstClaim = executor.submit(() ->
                    claimAfterSignal(firstToken, claimedAt, staleBefore, workersReady, startSignal));
            Future<Integer> secondClaim = executor.submit(() ->
                    claimAfterSignal(secondToken, claimedAt, staleBefore, workersReady, startSignal));

            assertTrue(workersReady.await(5, TimeUnit.SECONDS));
            startSignal.countDown();

            assertEquals(20, firstClaim.get(10, TimeUnit.SECONDS)
                    + secondClaim.get(10, TimeUnit.SECONDS));
        } finally {
            startSignal.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        Set<Long> firstBatch = deliveryIds(firstToken);
        Set<Long> secondBatch = deliveryIds(secondToken);

        assertEquals(10, firstBatch.size());
        assertEquals(10, secondBatch.size());
        assertTrue(Collections.disjoint(firstBatch, secondBatch));
        assertEquals(20, jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_delivery "
                        + "WHERE subscription_id = ? AND status = 'PROCESSING' AND attempts = 2",
                Integer.class,
                subscriptionId));
    }

    private int claimAfterSignal(
            String token,
            Instant claimedAt,
            Instant staleBefore,
            CountDownLatch workersReady,
            CountDownLatch startSignal) throws InterruptedException {
        workersReady.countDown();
        assertTrue(startSignal.await(5, TimeUnit.SECONDS));
        return deliveries.claimRetryBatch(token, claimedAt, staleBefore, 10);
    }

    private Set<Long> deliveryIds(String claimToken) {
        return deliveries.findAllByClaimToken(claimToken).stream()
                .map(NotificationDelivery::getId)
                .collect(Collectors.toSet());
    }

    private long createUser(String suffix) {
        String googleId = TEST_USER_PREFIX + suffix;
        jdbc.update(
                "INSERT INTO users (google_id, email, name) VALUES (?, ?, ?)",
                googleId, suffix + "@example.com", "Repository Integration Test");
        return jdbc.queryForObject(
                "SELECT id FROM users WHERE google_id = ?",
                Long.class,
                googleId);
    }

    private long createSubscription(long userId, String name) {
        return jdbc.queryForObject("""
                INSERT INTO subscription (
                    name, cost, currency, type, category, start_date,
                    billing_interval_unit, billing_interval_count,
                    email_notifications_enabled, user_id
                ) VALUES (?, 10.0000, 'USD', 'RECURRING', 'SOFTWARE', CURRENT_DATE,
                    'MONTH', 1, TRUE, ?)
                RETURNING id
                """, Long.class, name, userId);
    }

    private void seedDueDeliveries(long subscriptionId, Instant claimedAt, int count) {
        for (int index = 0; index < count; index++) {
            jdbc.update("""
                    INSERT INTO notification_delivery (
                        subscription_id, billing_date, notification_type, status, attempts,
                        created_at, next_attempt_at, last_attempt_at
                    ) VALUES (?, ?, 'UPCOMING_PAYMENT', 'RETRY_SCHEDULED', 1, ?, ?, ?)
                    """,
                    subscriptionId,
                    LocalDate.of(2026, 8, 1).plusDays(index),
                    Timestamp.from(claimedAt.minusSeconds(3600)),
                    Timestamp.from(claimedAt.minusSeconds(60)),
                    Timestamp.from(claimedAt.minusSeconds(3600)));
        }
    }
}
