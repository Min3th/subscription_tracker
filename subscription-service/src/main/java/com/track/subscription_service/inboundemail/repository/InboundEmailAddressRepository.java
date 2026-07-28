package com.track.subscription_service.inboundemail.repository;

import com.track.subscription_service.inboundemail.entity.InboundEmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundEmailAddressRepository extends JpaRepository<InboundEmailAddress, Long> {
}
