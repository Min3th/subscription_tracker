package com.track.subscription_service.auth.service;

import com.track.subscription_service.auth.entity.RefreshTokenSession;
import com.track.subscription_service.auth.repository.RefreshTokenSessionRepository;
import com.track.subscription_service.auth.util.AuthResponse;
import com.track.subscription_service.user.entity.User;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_DAYS = 7;

    private final RefreshTokenSessionRepository repository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenSessionRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse createSession(User user) {
        IssuedRefreshToken issued = issueRefreshToken(user);
        return new AuthResponse(jwtService.generateAccessToken(user), issued.token(), user);
    }

    @Transactional(noRollbackFor = RefreshTokenCompromiseException.class)
    public AuthResponse rotate(String refreshToken) {
        Claims claims = jwtService.validateRefreshToken(refreshToken);
        String presentedHash = hash(refreshToken);
        RefreshTokenSession current = repository.findByTokenHash(presentedHash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh session not found"));

        Instant now = Instant.now();
        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)) {
            // Reuse of a rotated token is treated as session-family compromise.
            revokeAllForUser(current.getUser());
            throw new RefreshTokenCompromiseException("Refresh session is no longer active");
        }
        if (!current.getTokenId().equals(claims.getId()) ||
                !current.getUser().getGoogleId().equals(claims.getSubject())) {
            revokeAllForUser(current.getUser());
            throw new RefreshTokenCompromiseException("Refresh session does not match token claims");
        }

        IssuedRefreshToken replacement = issueRefreshToken(current.getUser());
        current.setRevokedAt(now);
        current.setReplacedByTokenId(replacement.tokenId());
        repository.save(current);

        return new AuthResponse(
                jwtService.generateAccessToken(current.getUser()),
                replacement.token(),
                current.getUser()
        );
    }

    @Transactional
    public void revoke(String refreshToken) {
        repository.findByTokenHash(hash(refreshToken)).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
                repository.save(session);
            }
        });
    }

    @Transactional
    public void revokeAllForUser(User user) {
        repository.revokeAllActiveForUser(user.getId(), Instant.now());
    }

    private IssuedRefreshToken issueRefreshToken(User user) {
        String tokenId = UUID.randomUUID().toString();
        String token = jwtService.generateRefreshToken(user, tokenId);
        Instant now = Instant.now();

        RefreshTokenSession session = new RefreshTokenSession();
        session.setTokenId(tokenId);
        session.setTokenHash(hash(token));
        session.setUser(user);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS));
        repository.save(session);

        return new IssuedRefreshToken(tokenId, token);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private record IssuedRefreshToken(String tokenId, String token) { }

    private static final class RefreshTokenCompromiseException extends IllegalArgumentException {
        private RefreshTokenCompromiseException(String message) {
            super(message);
        }
    }
}
