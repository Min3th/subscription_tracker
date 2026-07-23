package com.track.subscription_service.notification.repository;

import com.track.subscription_service.notification.entity.EmailSuppression;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

public interface EmailSuppressionRepository extends JpaRepository<EmailSuppression, Long> {
    boolean existsByEmailNormalized(String emailNormalized);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO email_suppression (email_normalized, reason, source, created_at, updated_at)
            VALUES (:email, :reason, :source, :now, :now)
            ON CONFLICT (email_normalized) DO UPDATE
            SET reason = EXCLUDED.reason, source = EXCLUDED.source, updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsert(@Param("email") String email, @Param("reason") String reason,
                @Param("source") String source, @Param("now") Instant now);
}
