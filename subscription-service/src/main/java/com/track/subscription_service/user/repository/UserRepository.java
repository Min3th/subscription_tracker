package com.track.subscription_service.user.repository;

import com.track.subscription_service.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByGoogleId(String googleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT user FROM User user WHERE user.googleId = :googleId")
    Optional<User> findByGoogleIdForUpdate(@Param("googleId") String googleId);

    List<User> findAllByEmailIgnoreCase(String email);
    // Spring parses this automatically to -> SELECT * FROM users WHERE google_id = ?
}
