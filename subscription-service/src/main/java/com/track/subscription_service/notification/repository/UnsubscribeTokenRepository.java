package com.track.subscription_service.notification.repository;

import com.track.subscription_service.notification.entity.UnsubscribeToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

public interface UnsubscribeTokenRepository extends JpaRepository<UnsubscribeToken, Long> {
    Optional<UnsubscribeToken> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(String hash, Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE UnsubscribeToken token SET token.usedAt = :usedAt WHERE token.id = :id AND token.usedAt IS NULL")
    int markUsed(@Param("id") Long id, @Param("usedAt") Instant usedAt);
}
