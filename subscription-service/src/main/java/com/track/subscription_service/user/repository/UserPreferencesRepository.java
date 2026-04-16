package com.track.subscription_service.user.repository;

import com.track.subscription_service.user.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences,Long> {
}
