package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class InboundEmailTokenCodec {
    private static final int TOKEN_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String CIPHERTEXT_VERSION = "v1";

    private final InboundEmailProperties properties;
    private final SecureRandom secureRandom;

    @Autowired
    public InboundEmailTokenCodec(InboundEmailProperties properties) {
        this(properties, new SecureRandom());
    }

    InboundEmailTokenCodec(InboundEmailProperties properties, SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public String generateToken() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Inbound email token is required");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public String encrypt(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Inbound email token is required");
        }
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return String.join(".",
                    CIPHERTEXT_VERSION,
                    Base64.getUrlEncoder().withoutPadding().encodeToString(iv),
                    Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to protect inbound email token", exception);
        }
    }

    public String decrypt(String protectedToken) {
        if (protectedToken == null) {
            throw new IllegalArgumentException("Protected inbound email token is required");
        }
        String[] parts = protectedToken.split("\\.", -1);
        if (parts.length != 3 || !CIPHERTEXT_VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported inbound email token format");
        }
        try {
            byte[] iv = Base64.getUrlDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[2]);
            if (iv.length != IV_BYTES) {
                throw new IllegalArgumentException("Invalid inbound email token");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (AEADBadTagException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid inbound email token", exception);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to read inbound email token", exception);
        }
    }

    private SecretKeySpec encryptionKey() {
        String configuredKey = properties.getTokenEncryptionKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("Inbound email token encryption key is not configured");
        }
        try {
            byte[] decodedKey = Base64.getDecoder().decode(configuredKey);
            if (decodedKey.length != 32) {
                throw new IllegalStateException(
                        "Inbound email token encryption key must decode to exactly 32 bytes");
            }
            return new SecretKeySpec(decodedKey, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Inbound email token encryption key must be valid Base64", exception);
        }
    }
}
