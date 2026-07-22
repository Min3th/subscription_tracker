package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.entity.UnsubscribeToken;
import com.track.subscription_service.notification.repository.UnsubscribeTokenRepository;
import com.track.subscription_service.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class UnsubscribeService {
    private static final Duration TOKEN_LIFETIME = Duration.ofDays(90);
    private final UnsubscribeTokenRepository tokenRepository;
    private final EmailSuppressionService suppressionService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String publicApiUrl;

    public UnsubscribeService(UnsubscribeTokenRepository tokenRepository,
                              EmailSuppressionService suppressionService, Clock clock,
                              @Value("${app.public-api-url}") String publicApiUrl) {
        this.tokenRepository = tokenRepository;
        this.suppressionService = suppressionService;
        this.clock = clock;
        this.publicApiUrl = publicApiUrl.replaceAll("/+$", "");
    }

    @Transactional
    public String createLink(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        UnsubscribeToken token = new UnsubscribeToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(TOKEN_LIFETIME));
        tokenRepository.save(token);
        return publicApiUrl + "/notifications/unsubscribe?token=" + rawToken;
    }

    @Transactional
    public void unsubscribe(String rawToken) {
        Instant now = clock.instant();
        UnsubscribeToken token = tokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(hash(rawToken), now)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired unsubscribe link"));
        if (tokenRepository.markUsed(token.getId(), now) != 1) {
            throw new IllegalArgumentException("Invalid or expired unsubscribe link");
        }
        suppressionService.suppress(token.getUser().getEmail(), "UNSUBSCRIBE", "ONE_CLICK");
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
