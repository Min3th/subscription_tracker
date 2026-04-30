package com.track.subscription_service.notification.config;

import com.sendgrid.SendGrid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SendGridConfig {

    private final SendGridProperties props;

    public SendGridConfig(SendGridProperties props) {
        this.props = props;
    }

    @Bean
    public SendGrid sendGrid() {
        return new SendGrid(props.getApiKey());
    }
}