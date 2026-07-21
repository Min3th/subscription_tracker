package com.track.subscription_service.subscription.entity;


import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.model.SubscriptionCategory;
import com.track.subscription_service.subscription.model.SubscriptionType;
import jakarta.persistence.*;
import java.time.LocalDate;


@Entity
@Table(name = "subscription")
public class Subscription {
    public SubscriptionCategory getCategory() {
        return category;
    }

    public void setCategory(SubscriptionCategory category) {
        this.category = category;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Double cost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionType type;

    @Column(length = 50)
    private String duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionCategory category;

    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private boolean emailNotificationsEnabled;

    @Column(name = "payment_method", length = 120)
    private String paymentMethod;

    @Column(length = 500)
    private String website;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval_unit", length = 10)
    private BillingUnit billingIntervalUnit;

    @Column(name = "billing_interval_count")
    private Integer billingIntervalCount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Subscription(){}
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SubscriptionType getType() {
        return type;
    }

    public void setType(SubscriptionType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public BillingUnit getBillingIntervalUnit() {
        return billingIntervalUnit;
    }

    public void setBillingIntervalUnit(BillingUnit billingIntervalUnit) {
        this.billingIntervalUnit = billingIntervalUnit;
    }

    public Integer getBillingIntervalCount() {
        return billingIntervalCount;
    }

    public void setBillingIntervalCount(Integer billingIntervalCount) {
        this.billingIntervalCount = billingIntervalCount;
    }

    public Boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }
}
