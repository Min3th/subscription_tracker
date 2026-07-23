package com.track.subscription_service.subscription.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SubscriptionCategory {
    GENERAL("General"), ENTERTAINMENT("Entertainment"), PRODUCTIVITY("Productivity"),
    MUSIC("Music"), SOFTWARE("Software"), UTILITIES("Utilities"), EDUCATION("Education"),
    HEALTH("Health"), FITNESS("Fitness"), NEWS("News"), CLOUD_STORAGE("Cloud Storage"),
    FINANCE("Finance"), OTHER("Other");

    private final String value;

    SubscriptionCategory(String value) { this.value = value; }

    @JsonValue
    public String value() { return value; }

    @JsonCreator
    public static SubscriptionCategory fromValue(String value) {
        String normalized = value == null ? "" : value.trim().replace('_', ' ');
        for (SubscriptionCategory category : values()) {
            if (category.value.equalsIgnoreCase(normalized)) return category;
        }
        throw new IllegalArgumentException("Unsupported subscription category: " + value);
    }
}
