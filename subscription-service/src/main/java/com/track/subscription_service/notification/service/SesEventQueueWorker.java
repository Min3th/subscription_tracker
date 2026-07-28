package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.config.SesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Service
@ConditionalOnProperty(
        name = "app.email.ses-consumers-enabled",
        havingValue = "true")
public class SesEventQueueWorker {
    private static final Logger log =
            LoggerFactory.getLogger(SesEventQueueWorker.class);

    private final SqsClient sqsClient;
    private final SesEventService eventService;
    private final String queueUrl;

    public SesEventQueueWorker(
            SqsClient sqsClient,
            SesEventService eventService,
            SesProperties properties) {
        this.sqsClient = sqsClient;
        this.eventService = eventService;
        this.queueUrl = properties.getEventQueueUrl();
    }

    @Scheduled(
            fixedDelayString = "${app.email.ses.event-poll-delay-ms:1000}",
            initialDelayString = "${app.email.ses.event-poll-initial-delay-ms:5000}")
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
        } catch (SqsException exception) {
            log.error("Unable to poll the SES event queue", exception);
        }
    }

    private void process(Message message) {
        try {
            eventService.process(message.body());
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (RuntimeException exception) {
            log.error(
                    "SES event processing failed for SQS message {}; leaving it for retry",
                    message.messageId(),
                    exception);
        }
    }
}
