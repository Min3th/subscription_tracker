package com.track.subscription_service.subscription.repository;

import com.track.subscription_service.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {
    List<Subscription> findByUser_GoogleId(String googleId);
    Optional<Subscription> findByIdAndUser_GoogleId(Long id,String googleId);

    @EntityGraph(attributePaths = {"user", "user.preferences"})
    Page<Subscription> findAllBy(Pageable pageable);
}
