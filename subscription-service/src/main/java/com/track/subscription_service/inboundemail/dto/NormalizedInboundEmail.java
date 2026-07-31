package com.track.subscription_service.inboundemail.dto;

public record NormalizedInboundEmail(
        String subject,
        String senderDomain,
        String body
) {
    public String searchableText() {
        return (subject == null ? "" : subject) + "\n" + body;
    }
}
