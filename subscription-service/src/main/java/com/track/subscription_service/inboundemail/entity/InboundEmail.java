package com.track.subscription_service.inboundemail.entity;

import com.track.subscription_service.inboundemail.model.InboundEmailStatus;
import com.track.subscription_service.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbound_email", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_inbound_email_recipient_fingerprint",
                columnNames = {"recipient_address_id", "message_fingerprint"}
        )
}, indexes = {
        @Index(name = "idx_inbound_email_user_received", columnList = "user_id, received_at"),
        @Index(name = "idx_inbound_email_worker", columnList = "status, next_attempt_at, received_at")
})
public class InboundEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_address_id", nullable = false)
    private InboundEmailAddress recipientAddress;

    @Column(name = "provider_message_id", length = 998)
    private String providerMessageId;

    @Column(name = "message_fingerprint", nullable = false, length = 64)
    private String messageFingerprint;

    @Column(name = "envelope_from", length = 320)
    private String envelopeFrom;

    @Column(length = 998)
    private String subject;

    @Column(name = "text_body", columnDefinition = "TEXT")
    private String textBody;

    @Column(name = "html_body", columnDefinition = "TEXT")
    private String htmlBody;

    @Column(name = "raw_headers", columnDefinition = "TEXT")
    private String rawHeaders;

    @Column(name = "spam_score", precision = 8, scale = 3)
    private BigDecimal spamScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InboundEmailStatus status;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "content_purged_at")
    private Instant contentPurgedAt;

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public InboundEmailAddress getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(InboundEmailAddress recipientAddress) {
        this.recipientAddress = recipientAddress;
    }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }
    public String getMessageFingerprint() { return messageFingerprint; }
    public void setMessageFingerprint(String messageFingerprint) {
        this.messageFingerprint = messageFingerprint;
    }
    public String getEnvelopeFrom() { return envelopeFrom; }
    public void setEnvelopeFrom(String envelopeFrom) { this.envelopeFrom = envelopeFrom; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getTextBody() { return textBody; }
    public void setTextBody(String textBody) { this.textBody = textBody; }
    public String getHtmlBody() { return htmlBody; }
    public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }
    public String getRawHeaders() { return rawHeaders; }
    public void setRawHeaders(String rawHeaders) { this.rawHeaders = rawHeaders; }
    public BigDecimal getSpamScore() { return spamScore; }
    public void setSpamScore(BigDecimal spamScore) { this.spamScore = spamScore; }
    public InboundEmailStatus getStatus() { return status; }
    public void setStatus(InboundEmailStatus status) { this.status = status; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Instant getProcessingStartedAt() { return processingStartedAt; }
    public void setProcessingStartedAt(Instant processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getContentPurgedAt() { return contentPurgedAt; }
    public void setContentPurgedAt(Instant contentPurgedAt) {
        this.contentPurgedAt = contentPurgedAt;
    }
}
