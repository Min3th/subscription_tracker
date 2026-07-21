package com.track.subscription_service.auth.repository;

import com.track.subscription_service.auth.entity.RefreshTokenSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenSession s set s.revokedAt = :revokedAt " +
            "where s.user.id = :userId and s.revokedAt is null")
    int revokeAllActiveForUser(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);
}
