package com.track.subscription_service.inboundemail.dto;

import java.math.BigDecimal;

public record ParsedInboundEmail(
        String envelope,
        String from,
        String to,
        String subject,
        String text,
        String html,
        String headers,
        BigDecimal spamScore,
        int attachmentCount
) {
}
