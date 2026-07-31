package com.track.subscription_service.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@ConditionalOnProperty(name = "app.email.outbound-provider", havingValue = "ses")
public class SesConfig {
    @Bean
    public SesV2Client sesV2Client(SesProperties properties) {
        requireText(properties.getRegion(), "SES region");
        requireText(properties.getFromEmail(), "SES sender email");
        requireText(properties.getFromName(), "SES sender name");
        requireText(properties.getConfigurationSet(), "SES configuration set");
        return SesV2Client.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(field + " must be configured");
        }
    }
}
