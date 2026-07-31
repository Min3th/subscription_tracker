package com.track.subscription_service.inboundemail.model;

public enum InboundEmailClassification {
    NEW_SUBSCRIPTION,
    RENEWAL_PAYMENT,
    UPCOMING_RENEWAL,
    PRICE_CHANGE,
    CANCELLATION,
    GMAIL_VERIFICATION,
    NOT_SUBSCRIPTION
}
