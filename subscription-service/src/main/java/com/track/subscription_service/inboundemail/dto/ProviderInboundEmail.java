package com.track.subscription_service.inboundemail.dto;

import java.time.Instant;
import java.util.List;

public record ProviderInboundEmail(
        String providerMessageId,
        String envelopeSender,
        List<String> envelopeRecipients,
        byte[] rawMime,
        String spamVerdict,
        String virusVerdict,
        Instant receivedAt
) {
    public ProviderInboundEmail {
        envelopeRecipients = envelopeRecipients == null
                ? List.of()
                : List.copyOf(envelopeRecipients);
        rawMime = rawMime == null ? null : rawMime.clone();
    }

    @Override
    public byte[] rawMime() {
        return rawMime == null ? null : rawMime.clone();
    }
}
