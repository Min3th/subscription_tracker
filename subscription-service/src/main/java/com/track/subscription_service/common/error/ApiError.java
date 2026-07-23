package com.track.subscription_service.common.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> errors
) {
    public record FieldViolation(String field, String message) { }
}
