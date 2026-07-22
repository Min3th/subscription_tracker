package com.track.subscription_service.notification.service;

import java.io.IOException;

public class EmailDeliveryException extends RuntimeException {

    private final Integer statusCode;

    private EmailDeliveryException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public static EmailDeliveryException providerRejected(int statusCode) {
        return new EmailDeliveryException(
                "Email provider rejected delivery with HTTP status " + statusCode,
                statusCode,
                null
        );
    }

    public static EmailDeliveryException transportFailure(IOException exception) {
        return new EmailDeliveryException("Email provider request failed", null, exception);
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
