package com.track.subscription_service.subscription.dto;

import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.model.SubscriptionCategory;
import com.track.subscription_service.subscription.model.SubscriptionType;
import java.time.LocalDate;

public class SubscriptionResponse {
    public Long id;
    public String name;
    public double cost;
    public SubscriptionType type;
    public String duration;
    public SubscriptionCategory category;
    public String description;
    public String paymentMethod;
    public String website;
    public LocalDate startDate;
    public BillingUnit billingIntervalUnit;
    public Integer billingIntervalCount;
    public LocalDate nextBillingDate;
    public double totalPaid;
}
