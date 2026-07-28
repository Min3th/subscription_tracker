package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.InboundEmailAddressResponse;
import com.track.subscription_service.inboundemail.entity.InboundEmailAddress;
import com.track.subscription_service.inboundemail.repository.InboundEmailAddressRepository;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InboundEmailAddressServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-28T04:00:00Z");

    @Test
    void createsAndPersistsAProtectedAddress() {
        InboundEmailAddressRepository addressRepository = mock(InboundEmailAddressRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = user();
        when(userRepository.findByGoogleIdForUpdate("google-user")).thenReturn(Optional.of(user));
        when(addressRepository.findByUserGoogleIdAndRevokedAtIsNull("google-user"))
                .thenReturn(Optional.empty());
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        InboundEmailAddressService service = service(addressRepository, userRepository);

        InboundEmailAddressResponse response = service.createIfAbsent("google-user");

        var saved = org.mockito.ArgumentCaptor.forClass(InboundEmailAddress.class);
        verify(addressRepository).save(saved.capture());
        String rawToken = response.address().substring(4, response.address().indexOf('@'));
        assertTrue(response.active());
        assertTrue(response.address().matches("^sub-[A-Za-z0-9_-]{43}@inbound\\.subtrak\\.me$"));
        assertEquals(NOW, response.createdAt());
        assertEquals(64, saved.getValue().getTokenHash().length());
        assertFalse(saved.getValue().getEncryptedToken().contains(rawToken));
        assertNotEquals(rawToken, saved.getValue().getTokenHash());
    }

    @Test
    void returnsTheExistingAddressWithoutCreatingAnother() {
        InboundEmailAddressRepository addressRepository = mock(InboundEmailAddressRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        InboundEmailTokenCodec codec = codec();
        InboundEmailAddress existing = new InboundEmailAddress();
        existing.setEncryptedToken(codec.encrypt("existing-token"));
        existing.setCreatedAt(NOW.minusSeconds(60));
        when(userRepository.findByGoogleIdForUpdate("google-user")).thenReturn(Optional.of(user()));
        when(addressRepository.findByUserGoogleIdAndRevokedAtIsNull("google-user"))
                .thenReturn(Optional.of(existing));
        InboundEmailAddressService service = service(addressRepository, userRepository, codec);

        InboundEmailAddressResponse response = service.createIfAbsent("google-user");

        assertEquals("sub-existing-token@inbound.subtrak.me", response.address());
        verify(addressRepository, never()).save(any());
    }

    @Test
    void rotationRevokesAndFlushesTheExistingAddressBeforeCreatingANewOne() {
        InboundEmailAddressRepository addressRepository = mock(InboundEmailAddressRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        InboundEmailAddress existing = new InboundEmailAddress();
        when(userRepository.findByGoogleIdForUpdate("google-user")).thenReturn(Optional.of(user()));
        when(addressRepository.findByUserGoogleIdAndRevokedAtIsNull("google-user"))
                .thenReturn(Optional.of(existing));
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        InboundEmailAddressService service = service(addressRepository, userRepository);

        service.rotate("google-user");

        assertEquals(NOW, existing.getRevokedAt());
        var order = inOrder(addressRepository);
        order.verify(addressRepository).saveAndFlush(existing);
        order.verify(addressRepository).save(argThat(address -> address != existing));
    }

    private InboundEmailAddressService service(InboundEmailAddressRepository addressRepository,
                                               UserRepository userRepository) {
        return service(addressRepository, userRepository, codec());
    }

    private InboundEmailAddressService service(InboundEmailAddressRepository addressRepository,
                                               UserRepository userRepository,
                                               InboundEmailTokenCodec codec) {
        InboundEmailProperties properties = properties();
        return new InboundEmailAddressService(
                addressRepository,
                userRepository,
                codec,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private InboundEmailTokenCodec codec() {
        return new InboundEmailTokenCodec(properties());
    }

    private InboundEmailProperties properties() {
        InboundEmailProperties properties = new InboundEmailProperties();
        properties.setDomain("inbound.subtrak.me");
        properties.setTokenEncryptionKey(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        return properties;
    }

    private User user() {
        User user = new User();
        user.setGoogleId("google-user");
        return user;
    }
}
