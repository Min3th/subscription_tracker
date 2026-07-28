package com.track.subscription_service.inboundemail.repository;

import com.track.subscription_service.inboundemail.entity.InboundEmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InboundEmailAddressRepository extends JpaRepository<InboundEmailAddress, Long> {
    Optional<InboundEmailAddress> findByUserGoogleIdAndRevokedAtIsNull(String googleId);
    Optional<InboundEmailAddress> findByTokenHashAndRevokedAtIsNull(String tokenHash);
}
