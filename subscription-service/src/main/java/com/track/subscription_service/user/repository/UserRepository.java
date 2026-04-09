package com.track.subscription_service.user.repository;

import com.track.subscription_service.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByGoogleId(String googleId);
    // Spring parses this automatically to -> SELECT * FROM users WHERE google_id = ?
}
