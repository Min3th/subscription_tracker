package com.track.subscription_service.inboundemail.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.inbound-email")
public class InboundEmailProperties {

    @NotBlank
    @Pattern(regexp = "^(?=.{1,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$")
    private String domain;

    private String tokenEncryptionKey;
    private String webhookPublicKey;

    @Min(1024)
    @Max(25 * 1024 * 1024)
    private long maxRequestBytes = 10 * 1024 * 1024;

    @Min(1)
    @Max(5 * 1024 * 1024)
    private int maxFieldBytes = 1024 * 1024;

    @Min(1)
    @Max(100)
    private int maxParts = 30;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTokenEncryptionKey() {
        return tokenEncryptionKey;
    }

    public void setTokenEncryptionKey(String tokenEncryptionKey) {
        this.tokenEncryptionKey = tokenEncryptionKey;
    }

    public String getWebhookPublicKey() {
        return webhookPublicKey;
    }

    public void setWebhookPublicKey(String webhookPublicKey) {
        this.webhookPublicKey = webhookPublicKey;
    }

    public long getMaxRequestBytes() {
        return maxRequestBytes;
    }

    public void setMaxRequestBytes(long maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }

    public int getMaxFieldBytes() {
        return maxFieldBytes;
    }

    public void setMaxFieldBytes(int maxFieldBytes) {
        this.maxFieldBytes = maxFieldBytes;
    }

    public int getMaxParts() {
        return maxParts;
    }

    public void setMaxParts(int maxParts) {
        this.maxParts = maxParts;
    }
}
