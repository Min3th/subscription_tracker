package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class InboundEmailTokenCodecTest {

    private static final String VALID_KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void generatesUrlSafeTokensWithAtLeast256BitsOfEntropy() {
        InboundEmailTokenCodec codec = codec(VALID_KEY);

        String first = codec.generateToken();
        String second = codec.generateToken();

        assertEquals(32, Base64.getUrlDecoder().decode(first).length);
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
        assertNotEquals(first, second);
    }

    @Test
    void hashesTokensWithoutRetainingTheirRawValue() {
        InboundEmailTokenCodec codec = codec(VALID_KEY);

        String hash = codec.hash("raw-address-token");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertNotEquals("raw-address-token", hash);
        assertEquals(hash, codec.hash("raw-address-token"));
    }

    @Test
    void encryptsAndDecryptsWithoutExposingTheToken() {
        InboundEmailTokenCodec codec = codec(VALID_KEY);
        String rawToken = "private-forwarding-token";

        String encrypted = codec.encrypt(rawToken);

        assertTrue(encrypted.startsWith("v1."));
        assertFalse(encrypted.contains(rawToken));
        assertEquals(rawToken, codec.decrypt(encrypted));
        assertNotEquals(encrypted, codec.encrypt(rawToken));
    }

    @Test
    void rejectsTamperedCiphertext() {
        InboundEmailTokenCodec codec = codec(VALID_KEY);
        String encrypted = codec.encrypt("private-forwarding-token");
        String[] parts = encrypted.split("\\.");
        byte[] ciphertext = Base64.getUrlDecoder().decode(parts[2]);
        ciphertext[0] ^= 1;
        String tampered = parts[0] + "." + parts[1] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);

        assertThrows(IllegalArgumentException.class, () -> codec.decrypt(tampered));
    }

    @Test
    void requiresAValid256BitEncryptionKeyWhenEncryptionIsUsed() {
        assertThrows(IllegalStateException.class, () -> codec("").encrypt("token"));
        assertThrows(IllegalStateException.class, () -> codec("not-base64").encrypt("token"));
        assertThrows(IllegalStateException.class,
                () -> codec(Base64.getEncoder().encodeToString(new byte[16])).encrypt("token"));
    }

    private InboundEmailTokenCodec codec(String key) {
        InboundEmailProperties properties = new InboundEmailProperties();
        properties.setDomain("inbound.subtrak.me");
        properties.setTokenEncryptionKey(key);
        return new InboundEmailTokenCodec(properties);
    }
}
