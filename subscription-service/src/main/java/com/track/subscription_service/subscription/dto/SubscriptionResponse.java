package com.track.subscription_service.subscription.dto;

import java.time.LocalDate;

public class SubscriptionResponse {
    public Long id;
    public String name;
    public double cost;
    public String billingIntervalUnit;
    public int billingIntervalCount;
    public LocalDate startDate;
    public LocalDate nextBillingDate;
    public double totalPaid;
}
