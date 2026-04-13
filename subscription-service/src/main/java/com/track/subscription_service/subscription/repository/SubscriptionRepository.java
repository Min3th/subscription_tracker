package com.track.subscription_service.subscription.repository;

import com.track.subscription_service.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {
    List<Subscription> findByUser_GoogleId(String googleId);
    Optional<Subscription> findByIdAndUser_GoogleId(Long id,String googleId);
}
