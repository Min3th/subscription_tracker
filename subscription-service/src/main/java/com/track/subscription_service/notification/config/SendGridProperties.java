package com.track.subscription_service.notification.config;

import com.sendgrid.SendGrid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sendgrid")
public class SendGridProperties {

    private String apiKey;

    private String fromEmail;

    private String fromName;
    private String eventWebhookPublicKey;

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

    public String getEventWebhookPublicKey() { return eventWebhookPublicKey; }
    public void setEventWebhookPublicKey(String eventWebhookPublicKey) {
        this.eventWebhookPublicKey = eventWebhookPublicKey;
    }
}
