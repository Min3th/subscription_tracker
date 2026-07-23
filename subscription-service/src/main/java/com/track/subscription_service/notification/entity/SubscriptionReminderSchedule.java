package com.track.subscription_service.notification.entity;

import com.track.subscription_service.subscription.entity.Subscription;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "subscription_reminder_schedule", indexes =
        @Index(name = "idx_reminder_schedule_due_at", columnList = "due_at"))
public class SubscriptionReminderSchedule {

    @Id
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getSubscriptionId() { return subscriptionId; }
    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) { this.subscription = subscription; }
    public LocalDate getBillingDate() { return billingDate; }
    public void setBillingDate(LocalDate billingDate) { this.billingDate = billingDate; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
