package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.common.error.ResourceNotFoundException;
import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.InboundEmailAddressResponse;
import com.track.subscription_service.inboundemail.entity.InboundEmailAddress;
import com.track.subscription_service.inboundemail.repository.InboundEmailAddressRepository;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class InboundEmailAddressService {
    private static final String LOCAL_PART_PREFIX = "sub-";

    private final InboundEmailAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final InboundEmailTokenCodec tokenCodec;
    private final InboundEmailProperties properties;
    private final Clock clock;

    public InboundEmailAddressService(InboundEmailAddressRepository addressRepository,
                                      UserRepository userRepository,
                                      InboundEmailTokenCodec tokenCodec,
                                      InboundEmailProperties properties,
                                      Clock clock) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.tokenCodec = tokenCodec;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InboundEmailAddressResponse getActiveAddress(String googleId) {
        return addressRepository.findByUserGoogleIdAndRevokedAtIsNull(googleId)
                .map(this::toResponse)
                .orElseGet(InboundEmailAddressResponse::inactive);
    }

    @Transactional
    public InboundEmailAddressResponse createIfAbsent(String googleId) {
        User user = authenticatedUserForUpdate(googleId);
        return addressRepository.findByUserGoogleIdAndRevokedAtIsNull(googleId)
                .map(this::toResponse)
                .orElseGet(() -> createAddress(user, clock.instant()));
    }

    @Transactional
    public InboundEmailAddressResponse rotate(String googleId) {
        User user = authenticatedUserForUpdate(googleId);
        Instant now = clock.instant();
        addressRepository.findByUserGoogleIdAndRevokedAtIsNull(googleId).ifPresent(existing -> {
            existing.setRevokedAt(now);
            addressRepository.saveAndFlush(existing);
        });
        return createAddress(user, now);
    }

    @Transactional
    public void revoke(String googleId) {
        authenticatedUserForUpdate(googleId);
        addressRepository.findByUserGoogleIdAndRevokedAtIsNull(googleId).ifPresent(existing -> {
            existing.setRevokedAt(clock.instant());
            addressRepository.save(existing);
        });
    }

    private InboundEmailAddressResponse createAddress(User user, Instant createdAt) {
        String rawToken = tokenCodec.generateToken();
        InboundEmailAddress address = new InboundEmailAddress();
        address.setUser(user);
        address.setTokenHash(tokenCodec.hash(rawToken));
        address.setEncryptedToken(tokenCodec.encrypt(rawToken));
        address.setCreatedAt(createdAt);
        InboundEmailAddress saved = addressRepository.save(address);
        return response(rawToken, saved.getCreatedAt());
    }

    private User authenticatedUserForUpdate(String googleId) {
        return userRepository.findByGoogleIdForUpdate(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

    private InboundEmailAddressResponse toResponse(InboundEmailAddress address) {
        return response(tokenCodec.decrypt(address.getEncryptedToken()), address.getCreatedAt());
    }

    private InboundEmailAddressResponse response(String rawToken, Instant createdAt) {
        String domain = properties.getDomain().toLowerCase(Locale.ROOT);
        return new InboundEmailAddressResponse(
                true,
                LOCAL_PART_PREFIX + rawToken + "@" + domain,
                createdAt
        );
    }
}
