package com.track.subscription_service.subscription.repository;

import com.track.subscription_service.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {
}
