package com.track.subscription_service.subscription.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SubscriptionType {
    ONE_TIME("one-time"),
    RECURRING("recurring");

    private final String value;

    SubscriptionType(String value) { this.value = value; }

    @JsonValue
    public String value() { return value; }

    @JsonCreator
    public static SubscriptionType fromValue(String value) {
        for (SubscriptionType type : values()) {
            if (type.value.equalsIgnoreCase(value == null ? "" : value.trim())) return type;
        }
        throw new IllegalArgumentException("Unsupported subscription type: " + value);
    }
}
