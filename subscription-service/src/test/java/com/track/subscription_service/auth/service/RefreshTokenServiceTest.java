package com.track.subscription_service.auth.service;

import com.track.subscription_service.auth.entity.RefreshTokenSession;
import com.track.subscription_service.auth.repository.RefreshTokenSessionRepository;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    @Test
    void storesOnlyAHashOfTheRefreshToken() {
        RefreshTokenSessionRepository repository = mock(RefreshTokenSessionRepository.class);
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.generateRefreshToken(any(User.class), anyString())).thenReturn("raw-refresh-token");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");

        RefreshTokenService service = new RefreshTokenService(repository, jwtService);
        User user = new User();
        user.setId(1L);
        user.setGoogleId("google-user-123");

        var result = service.createSession(user);

        ArgumentCaptor<RefreshTokenSession> captor = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(repository).save(captor.capture());
        RefreshTokenSession stored = captor.getValue();
        assertEquals(64, stored.getTokenHash().length());
        assertNotEquals("raw-refresh-token", stored.getTokenHash());
        assertEquals("raw-refresh-token", result.getRefreshToken());
        assertEquals("access-token", result.getAccessToken());
    }
}
