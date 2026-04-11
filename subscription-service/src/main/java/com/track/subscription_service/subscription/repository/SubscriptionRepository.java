package com.track.subscription_service.subscription.repository;

import com.track.subscription_service.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {
    List<Subscription> findByUser_GoogleId(String googleId);
}
