package com.track.subscription_service.auth.entity;

import com.track.subscription_service.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_token_sessions", indexes = {
        @Index(name = "idx_refresh_session_user", columnList = "user_id"),
        @Index(name = "idx_refresh_session_expires", columnList = "expires_at")
})
public class RefreshTokenSession {

    @Id
    @Column(name = "token_id", nullable = false, length = 36)
    private String tokenId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_id", length = 36)
    private String replacedByTokenId;

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public String getReplacedByTokenId() { return replacedByTokenId; }
    public void setReplacedByTokenId(String replacedByTokenId) { this.replacedByTokenId = replacedByTokenId; }
}
