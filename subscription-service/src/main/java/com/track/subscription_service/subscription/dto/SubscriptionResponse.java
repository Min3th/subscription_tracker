package com.track.subscription_service.subscription.dto;

import java.time.LocalDate;

public class SubscriptionResponse {
    public Long id;
    public String name;
    public double cost;
    public String type;
    public String duration;
    public String category;
    public String description;
    public String paymentMethod;
    public String website;
    public LocalDate startDate;
    public String billingIntervalUnit;
    public int billingIntervalCount;
    public LocalDate nextBillingDate;
    public double totalPaid;
}
