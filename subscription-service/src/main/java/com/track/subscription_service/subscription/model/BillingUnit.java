package com.track.subscription_service.subscription.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BillingUnit {
    DAY("day"), WEEK("week"), MONTH("month"), YEAR("year");

    private final String value;

    BillingUnit(String value) { this.value = value; }

    @JsonValue
    public String value() { return value; }

    @JsonCreator
    public static BillingUnit fromValue(String value) {
        for (BillingUnit unit : values()) {
            if (unit.value.equalsIgnoreCase(value == null ? "" : value.trim())) return unit;
        }
        throw new IllegalArgumentException("Unsupported billing unit: " + value);
    }
}
