package com.track.subscription_service.subscription.dto;

import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.model.SubscriptionCategory;
import com.track.subscription_service.subscription.model.SubscriptionType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SubscriptionResponse {
    public Long id;
    public String name;
    @JsonSerialize(using = ToStringSerializer.class)
    public BigDecimal cost;
    public String currency;
    public SubscriptionType type;
    public String duration;
    public SubscriptionCategory category;
    public String description;
    public String paymentMethod;
    public String website;
    public LocalDate startDate;
    public BillingUnit billingIntervalUnit;
    public Integer billingIntervalCount;
    public boolean emailNotificationsEnabled;
    public LocalDate nextBillingDate;
    @JsonSerialize(using = ToStringSerializer.class)
    public BigDecimal totalPaid;
}
