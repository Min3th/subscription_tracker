package com.track.subscription_service.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(
        name = "app.email.ses-consumers-enabled",
        havingValue = "true")
public class SesConsumerConfig {

    @Bean
    public SqsClient sesSqsClient(SesProperties properties) {
        if (properties.getRegion() == null || properties.getRegion().isBlank()) {
            throw new IllegalStateException("SES region is required when SES consumers are enabled");
        }
        if (properties.getEventQueueUrl() == null
                || properties.getEventQueueUrl().isBlank()) {
            throw new IllegalStateException(
                    "SES event queue URL is required when SES consumers are enabled");
        }
        if (properties.getInboundQueueUrl() == null
                || properties.getInboundQueueUrl().isBlank()) {
            throw new IllegalStateException(
                    "SES inbound queue URL is required when SES consumers are enabled");
        }
        if (properties.getInboundBucket() == null
                || properties.getInboundBucket().isBlank()) {
            throw new IllegalStateException(
                    "SES inbound bucket is required when SES consumers are enabled");
        }
        return SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }

    @Bean
    public S3Client sesInboundS3Client(SesProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
