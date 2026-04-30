package com.track.subscription_service.notification.config;

import com.sendgrid.SendGrid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.sendgrid")
public class SendGridProperties {

    @NotBlank
    @Pattern(regexp = "^SG[0-9a-zA-Z._]{67}$")
    private String apiKey;

    @Email
    @NotBlank
    private String fromEmail;

    @NotBlank
    private String fromName;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}