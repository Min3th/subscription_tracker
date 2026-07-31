package com.track.subscription_service.inboundemail.dto;

import java.time.Instant;

public record InboundEmailAddressResponse(
        boolean active,
        String address,
        Instant createdAt
) {
    public static InboundEmailAddressResponse inactive() {
        return new InboundEmailAddressResponse(false, null, null);
    }
}
