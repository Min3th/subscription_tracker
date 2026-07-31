package com.track.subscription_service.notification.service;

public class EmailDeliveryException extends RuntimeException {

    private final Integer statusCode;
    private final boolean retryable;

    private EmailDeliveryException(
            String message, Integer statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public static EmailDeliveryException providerRejected(int statusCode) {
        return providerRejected(statusCode, statusCode == 429 || statusCode >= 500);
    }

    public static EmailDeliveryException providerRejected(
            int statusCode, boolean retryable) {
        return new EmailDeliveryException(
                "Email provider rejected delivery with HTTP status " + statusCode,
                statusCode,
                retryable,
                null
        );
    }

    public static EmailDeliveryException transportFailure(Exception exception) {
        return new EmailDeliveryException(
                "Email provider request failed", null, true, exception);
    }

    public static EmailDeliveryException messageConstructionFailure(Exception exception) {
        return new EmailDeliveryException(
                "Email message could not be constructed", null, false, exception);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
