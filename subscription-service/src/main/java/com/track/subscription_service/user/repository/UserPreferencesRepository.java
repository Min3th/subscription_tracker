package com.track.subscription_service.user.repository;

import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences,Long> {
    Optional<UserPreferences> findByUser(User user);
}
