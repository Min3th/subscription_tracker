package com.track.subscription_service.inboundemail.repository;

import com.track.subscription_service.inboundemail.entity.InboundEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboundEmailRepository extends JpaRepository<InboundEmail, UUID> {
    boolean existsByRecipientAddressIdAndMessageFingerprint(
            Long recipientAddressId,
            String messageFingerprint
    );
}
