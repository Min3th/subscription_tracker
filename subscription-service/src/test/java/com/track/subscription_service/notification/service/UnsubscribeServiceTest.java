package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.entity.UnsubscribeToken;
import com.track.subscription_service.notification.repository.UnsubscribeTokenRepository;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UnsubscribeServiceTest {
    private final Instant now = Instant.parse("2026-07-22T03:30:00Z");

    @Test
    void createsAHashedExpiringTokenAndNeverStoresTheRawToken() {
        UnsubscribeTokenRepository repository = mock(UnsubscribeTokenRepository.class);
        UnsubscribeService service = new UnsubscribeService(repository, mock(EmailSuppressionService.class),
                Clock.fixed(now, ZoneOffset.UTC), "https://api.example.com/");
        User user = new User();

        String link = service.createLink(user);

        String rawToken = link.substring(link.indexOf("token=") + 6);
        ArgumentCaptor<UnsubscribeToken> saved = ArgumentCaptor.forClass(UnsubscribeToken.class);
        verify(repository).save(saved.capture());
        assertEquals(64, saved.getValue().getTokenHash().length());
        assertNotEquals(rawToken, saved.getValue().getTokenHash());
        assertEquals(now.plus(Duration.ofDays(90)), saved.getValue().getExpiresAt());
        assertTrue(link.startsWith("https://api.example.com/notifications/unsubscribe?token="));
    }

    @Test
    void consumesTokenBeforeSuppressingRecipient() {
        UnsubscribeTokenRepository repository = mock(UnsubscribeTokenRepository.class);
        EmailSuppressionService suppressionService = mock(EmailSuppressionService.class);
        UnsubscribeService service = new UnsubscribeService(repository, suppressionService,
                Clock.fixed(now, ZoneOffset.UTC), "https://api.example.com");
        User user = new User();
        user.setEmail("USER@Example.com");
        UnsubscribeToken token = mock(UnsubscribeToken.class);
        when(token.getId()).thenReturn(7L);
        when(token.getUser()).thenReturn(user);
        when(repository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(anyString(), eq(now)))
                .thenReturn(Optional.of(token));
        when(repository.markUsed(7L, now)).thenReturn(1);

        service.unsubscribe("raw-token");

        verify(repository).markUsed(7L, now);
        verify(suppressionService).suppress("USER@Example.com", "UNSUBSCRIBE", "ONE_CLICK");
    }

    @Test
    void rejectsExpiredOrAlreadyUsedToken() {
        UnsubscribeTokenRepository repository = mock(UnsubscribeTokenRepository.class);
        UnsubscribeService service = new UnsubscribeService(repository, mock(EmailSuppressionService.class),
                Clock.fixed(now, ZoneOffset.UTC), "https://api.example.com");

        assertThrows(IllegalArgumentException.class, () -> service.unsubscribe("invalid"));
    }
}
