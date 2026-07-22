package com.track.subscription_service.notification.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_suppression")
public class EmailSuppression {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "email_normalized", nullable = false, unique = true, length = 320)
    private String emailNormalized;
    @Column(nullable = false, length = 30)
    private String reason;
    @Column(nullable = false, length = 30)
    private String source;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public String getEmailNormalized() { return emailNormalized; }
    public String getReason() { return reason; }
    public String getSource() { return source; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
