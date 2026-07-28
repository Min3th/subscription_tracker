package com.track.subscription_service.inboundemail.service;

import com.sendgrid.helpers.eventwebhook.EventWebhook;
import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InboundEmailWebhookVerifierTest {
    private static final Instant NOW = Instant.parse("2026-07-28T04:00:00Z");

    @Test
    void verifiesTheExactRawBytesBeforeTheyAreParsed() throws Exception {
        EventWebhook eventWebhook = mock(EventWebhook.class);
        ECPublicKey publicKey = mock(ECPublicKey.class);
        when(eventWebhook.ConvertPublicKeyToECDSA("public-key")).thenReturn(publicKey);
        when(eventWebhook.VerifySignature(
                eq(publicKey), any(byte[].class), eq("signature"), eq(epochTimestamp())))
                .thenReturn(true);
        InboundEmailWebhookVerifier verifier = verifier(eventWebhook);
        byte[] rawBody = new byte[] {0, 10, 13, (byte) 0xC3, (byte) 0xA9, (byte) 0xFF};

        verifier.verify(rawBody, "signature", epochTimestamp());

        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(eventWebhook).VerifySignature(
                eq(publicKey), bytes.capture(), eq("signature"), eq(epochTimestamp()));
        assertArrayEquals(rawBody, bytes.getValue());
    }

    @Test
    void rejectsMissingConfigurationAndStaleTimestamps() {
        InboundEmailProperties missing = new InboundEmailProperties();
        InboundEmailWebhookVerifier unconfigured = new InboundEmailWebhookVerifier(
                missing, Clock.fixed(NOW, ZoneOffset.UTC), mock(EventWebhook.class));

        assertThrows(SecurityException.class,
                () -> unconfigured.verify(new byte[] {1}, "signature", epochTimestamp()));

        EventWebhook eventWebhook = mock(EventWebhook.class);
        InboundEmailWebhookVerifier verifier = verifier(eventWebhook);
        String stale = Long.toString(NOW.minusSeconds(301).getEpochSecond());

        assertThrows(SecurityException.class,
                () -> verifier.verify(new byte[] {1}, "signature", stale));
        verifyNoInteractions(eventWebhook);
    }

    @Test
    void rejectsAnInvalidSignature() throws Exception {
        EventWebhook eventWebhook = mock(EventWebhook.class);
        ECPublicKey publicKey = mock(ECPublicKey.class);
        when(eventWebhook.ConvertPublicKeyToECDSA("public-key")).thenReturn(publicKey);
        when(eventWebhook.VerifySignature(any(), any(byte[].class), anyString(), anyString()))
                .thenReturn(false);

        assertThrows(SecurityException.class,
                () -> verifier(eventWebhook).verify(
                        new byte[] {1}, "signature", epochTimestamp()));
    }

    private InboundEmailWebhookVerifier verifier(EventWebhook eventWebhook) {
        InboundEmailProperties properties = new InboundEmailProperties();
        properties.setWebhookPublicKey("public-key");
        return new InboundEmailWebhookVerifier(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                eventWebhook
        );
    }

    private String epochTimestamp() {
        return Long.toString(NOW.getEpochSecond());
    }
}
