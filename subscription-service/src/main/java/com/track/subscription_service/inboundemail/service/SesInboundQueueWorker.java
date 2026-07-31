package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.ProviderInboundEmail;
import com.track.subscription_service.inboundemail.dto.SesInboundObject;
import com.track.subscription_service.notification.config.SesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.io.IOException;

@Service
@ConditionalOnProperty(
        name = "app.email.ses-consumers-enabled",
        havingValue = "true")
public class SesInboundQueueWorker {
    private static final Logger log =
            LoggerFactory.getLogger(SesInboundQueueWorker.class);

    private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final SesInboundNotificationParser notificationParser;
    private final InboundEmailIngestionService ingestionService;
    private final InboundEmailProperties inboundProperties;
    private final String queueUrl;
    private final String bucketName;

    public SesInboundQueueWorker(
            SqsClient sqsClient,
            S3Client s3Client,
            SesInboundNotificationParser notificationParser,
            InboundEmailIngestionService ingestionService,
            InboundEmailProperties inboundProperties,
            SesProperties sesProperties) {
        this.sqsClient = sqsClient;
        this.s3Client = s3Client;
        this.notificationParser = notificationParser;
        this.ingestionService = ingestionService;
        this.inboundProperties = inboundProperties;
        this.queueUrl = sesProperties.getInboundQueueUrl();
        this.bucketName = sesProperties.getInboundBucket();
    }

    @Scheduled(
            fixedDelayString = "${app.email.ses.inbound-poll-delay-ms:1000}",
            initialDelayString = "${app.email.ses.inbound-poll-initial-delay-ms:5000}")
    public void poll() {
        try {
            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20)
                    .build());
            for (Message message : response.messages()) {
                process(message);
            }
        } catch (RuntimeException exception) {
            log.error("Unable to poll the SES inbound queue", exception);
        }
    }

    private void process(Message message) {
        try {
            SesInboundObject object = notificationParser.parse(message.body());
            validateBucket(object.bucketName());
            byte[] rawMime = download(object);
            ingestionService.ingest(new ProviderInboundEmail(
                    object.providerMessageId(),
                    object.envelopeSender(),
                    object.envelopeRecipients(),
                    rawMime,
                    object.spamVerdict(),
                    object.virusVerdict(),
                    object.receivedAt()));
            delete(message);
        } catch (RuntimeException exception) {
            log.error(
                    "SES inbound processing failed for SQS message {}; leaving it for retry",
                    message.messageId(),
                    exception);
        }
    }

    private byte[] download(SesInboundObject object) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(object.objectKey())
                .build();
        try (ResponseInputStream<GetObjectResponse> input = s3Client.getObject(request)) {
            long contentLength = input.response().contentLength();
            if (contentLength > inboundProperties.getMaxRequestBytes()) {
                throw new IllegalArgumentException(
                        "Inbound MIME message exceeds the size limit");
            }
            byte[] content = input.readNBytes(
                    Math.toIntExact(inboundProperties.getMaxRequestBytes()) + 1);
            if (content.length > inboundProperties.getMaxRequestBytes()) {
                throw new IllegalArgumentException(
                        "Inbound MIME message exceeds the size limit");
            }
            return content;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read inbound MIME object", exception);
        }
    }

    private void validateBucket(String notificationBucket) {
        if (!bucketName.equals(notificationBucket)) {
            throw new IllegalArgumentException(
                    "SES receipt references an unexpected S3 bucket");
        }
    }

    private void delete(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}
