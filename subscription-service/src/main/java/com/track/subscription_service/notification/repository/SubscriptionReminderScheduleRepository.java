package com.track.subscription_service.notification.repository;

import com.track.subscription_service.notification.entity.SubscriptionReminderSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SubscriptionReminderScheduleRepository
        extends JpaRepository<SubscriptionReminderSchedule, Long> {

    @EntityGraph(attributePaths = {"subscription", "subscription.user", "subscription.user.preferences"})
    List<SubscriptionReminderSchedule> findTop100ByDueAtLessThanEqualOrderByDueAtAsc(Instant now);
}
