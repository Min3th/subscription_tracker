package com.track.subscription_service.subscription.entity;

import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "subscription")
public class Subscription {
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String duration;
    private Double cost;
    private String category;
    private String description;

    @Column(name = "payment_method")
    private String paymentMethod;

    private String website;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "billing_interval_unit")
    private String billingIntervalUnit;

    @Column(name = "billing_interval_count")
    private Integer billingIntervalCount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Subscription(){}
    public Subscription(String name, String type, String duration, Double cost,String category, String description, String paymentMethod,String website, LocalDate startDate, String billingIntervalUnit, Integer billingIntervalCount) {
        this.name = name;
        this.type = type;
        this.duration = duration;
        this.cost = cost;
        this.category = category;
        this.description = description;
        this.paymentMethod = paymentMethod;
        this.website = website;
        this.startDate = startDate;
        this.billingIntervalUnit = billingIntervalUnit;
        this.billingIntervalCount = billingIntervalCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getBillingIntervalUnit() {
        return billingIntervalUnit;
    }

    public void setBillingIntervalUnit(String billingIntervalUnit) {
        this.billingIntervalUnit = billingIntervalUnit;
    }

    public Integer getBillingIntervalCount() {
        return billingIntervalCount;
    }

    public void setBillingIntervalCount(Integer billingIntervalCount) {
        this.billingIntervalCount = billingIntervalCount;
    }
}
