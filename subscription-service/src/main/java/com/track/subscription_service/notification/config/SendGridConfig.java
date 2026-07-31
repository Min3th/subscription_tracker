package com.track.subscription_service.notification.config;

import com.sendgrid.SendGrid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(
        name = "app.email.outbound-provider",
        havingValue = "sendgrid",
        matchIfMissing = true)
public class SendGridConfig {

    private final SendGridProperties props;

    public SendGridConfig(SendGridProperties props) {
        this.props = props;
    }

    @Bean
    public SendGrid sendGrid() {
        if (props.getApiKey() == null
                || !props.getApiKey().matches("^SG[0-9a-zA-Z._]{67}$")) {
            throw new IllegalStateException("SendGrid API key must be configured");
        }
        if (props.getFromEmail() == null || props.getFromEmail().isBlank()) {
            throw new IllegalStateException("SendGrid sender email must be configured");
        }
        if (props.getFromName() == null || props.getFromName().isBlank()) {
            throw new IllegalStateException("SendGrid sender name must be configured");
        }
        return new SendGrid(props.getApiKey());
    }
}
