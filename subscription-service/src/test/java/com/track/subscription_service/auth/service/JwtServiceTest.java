package com.track.subscription_service.auth.service;

import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-that-is-at-least-32-bytes-long",
                "test-issuer",
                "test-audience"
        );
        user = new User();
        user.setGoogleId("google-user-123");
    }

    @Test
    void accessTokenContainsRequiredClaimsAndPassesAccessValidation() {
        String token = jwtService.generateAccessToken(user);
        var claims = jwtService.validateAccessToken(token);

        assertEquals("google-user-123", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
        assertEquals("test-issuer", claims.getIssuer());
        assertEquals("test-audience", claims.getAudience());
        assertNotNull(claims.getId());
    }

    @Test
    void refreshTokenCannotPassAccessValidation() {
        String token = jwtService.generateRefreshToken(user, "refresh-jti");

        assertThrows(IllegalArgumentException.class, () -> jwtService.validateAccessToken(token));
        assertEquals("refresh-jti", jwtService.validateRefreshToken(token).getId());
    }

    @Test
    void tokenFromWrongIssuerOrAudienceIsRejected() {
        JwtService otherIssuer = new JwtService(
                "test-secret-key-that-is-at-least-32-bytes-long",
                "other-issuer",
                "test-audience"
        );
        String token = otherIssuer.generateAccessToken(user);

        assertThrows(Exception.class, () -> jwtService.validateAccessToken(token));
    }
}
