package com.track.subscription_service.notification.repository;

import com.track.subscription_service.notification.entity.SubscriptionReminderSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SubscriptionReminderScheduleRepository
        extends JpaRepository<SubscriptionReminderSchedule, Long> {

    @EntityGraph(attributePaths = {"subscription", "subscription.user", "subscription.user.preferences"})
    List<SubscriptionReminderSchedule> findTop100ByDueAtLessThanEqualOrderByDueAtAsc(Instant now);

    @Modifying
    @Transactional
    @Query("DELETE FROM SubscriptionReminderSchedule schedule WHERE schedule.subscription.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
