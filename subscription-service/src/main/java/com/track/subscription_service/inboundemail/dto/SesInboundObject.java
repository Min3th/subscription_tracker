package com.track.subscription_service.inboundemail.dto;

import java.time.Instant;
import java.util.List;

public record SesInboundObject(
        String providerMessageId,
        String envelopeSender,
        List<String> envelopeRecipients,
        String spamVerdict,
        String virusVerdict,
        Instant receivedAt,
        String bucketName,
        String objectKey
) {
    public SesInboundObject {
        envelopeRecipients = envelopeRecipients == null
                ? List.of()
                : List.copyOf(envelopeRecipients);
    }
}
