package com.track.subscription_service.inboundemail.entity;

import com.track.subscription_service.inboundemail.model.SuggestionEventType;
import com.track.subscription_service.inboundemail.model.SuggestionStatus;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscription_suggestion", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_subscription_suggestion_inbound",
                columnNames = "inbound_email_id"
        )
}, indexes = {
        @Index(
                name = "idx_subscription_suggestion_user_status_created",
                columnList = "user_id, status, created_at"
        ),
        @Index(
                name = "idx_subscription_suggestion_possible_duplicate",
                columnList = "possible_duplicate_subscription_id"
        )
})
public class SubscriptionSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inbound_email_id", nullable = false)
    private InboundEmail inboundEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "possible_duplicate_subscription_id")
    private Subscription possibleDuplicateSubscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_subscription_id")
    private Subscription confirmedSubscription;

    @Column(nullable = false, length = 120)
    private String provider;

    @Column(name = "plan_name", length = 120)
    private String planName;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval_unit", length = 10)
    private BillingUnit billingIntervalUnit;

    @Column(name = "billing_interval_count")
    private Integer billingIntervalCount;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private SuggestionEventType eventType;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "evidence_summary", nullable = false, length = 1000)
    private String evidenceSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SuggestionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public InboundEmail getInboundEmail() { return inboundEmail; }
    public void setInboundEmail(InboundEmail inboundEmail) { this.inboundEmail = inboundEmail; }
    public Subscription getPossibleDuplicateSubscription() { return possibleDuplicateSubscription; }
    public void setPossibleDuplicateSubscription(Subscription subscription) {
        this.possibleDuplicateSubscription = subscription;
    }
    public Subscription getConfirmedSubscription() { return confirmedSubscription; }
    public void setConfirmedSubscription(Subscription subscription) {
        this.confirmedSubscription = subscription;
    }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BillingUnit getBillingIntervalUnit() { return billingIntervalUnit; }
    public void setBillingIntervalUnit(BillingUnit billingIntervalUnit) {
        this.billingIntervalUnit = billingIntervalUnit;
    }
    public Integer getBillingIntervalCount() { return billingIntervalCount; }
    public void setBillingIntervalCount(Integer billingIntervalCount) {
        this.billingIntervalCount = billingIntervalCount;
    }
    public LocalDate getRenewalDate() { return renewalDate; }
    public void setRenewalDate(LocalDate renewalDate) { this.renewalDate = renewalDate; }
    public SuggestionEventType getEventType() { return eventType; }
    public void setEventType(SuggestionEventType eventType) { this.eventType = eventType; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getEvidenceSummary() { return evidenceSummary; }
    public void setEvidenceSummary(String evidenceSummary) {
        this.evidenceSummary = evidenceSummary;
    }
    public SuggestionStatus getStatus() { return status; }
    public void setStatus(SuggestionStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
