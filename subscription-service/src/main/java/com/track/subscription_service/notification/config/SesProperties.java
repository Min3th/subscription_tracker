package com.track.subscription_service.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.email.ses")
public class SesProperties {
    private String region;
    private String fromEmail;
    private String fromName;
    private String configurationSet;
    private String inboundQueueUrl;
    private String eventQueueUrl;
    private String inboundBucket;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getConfigurationSet() { return configurationSet; }
    public void setConfigurationSet(String configurationSet) {
        this.configurationSet = configurationSet;
    }
    public String getInboundQueueUrl() { return inboundQueueUrl; }
    public void setInboundQueueUrl(String inboundQueueUrl) {
        this.inboundQueueUrl = inboundQueueUrl;
    }
    public String getEventQueueUrl() { return eventQueueUrl; }
    public void setEventQueueUrl(String eventQueueUrl) { this.eventQueueUrl = eventQueueUrl; }
    public String getInboundBucket() { return inboundBucket; }
    public void setInboundBucket(String inboundBucket) { this.inboundBucket = inboundBucket; }
}
