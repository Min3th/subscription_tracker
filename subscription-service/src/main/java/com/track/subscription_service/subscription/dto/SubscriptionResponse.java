package com.track.subscription_service.subscription.dto;

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
    @JsonSerialize(using = ToStringSerializer.class)
    public BigDecimal totalPaid;
}
