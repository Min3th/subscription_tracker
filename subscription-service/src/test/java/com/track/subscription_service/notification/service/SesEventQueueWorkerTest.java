package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.config.SesProperties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesEventQueueWorkerTest {

    @Test
    void successfulEventIsDeletedAfterProcessing() {
        SqsClient sqs = mock(SqsClient.class);
        SesEventService service = mock(SesEventService.class);
        Message message = message();
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        new SesEventQueueWorker(sqs, service, properties()).poll();

        verify(service).process(message.body());
        verify(sqs).deleteMessage(
                org.mockito.ArgumentMatchers.<software.amazon.awssdk.services.sqs.model.DeleteMessageRequest>argThat(
                        request -> "queue-url".equals(request.queueUrl())
                                && "receipt".equals(request.receiptHandle())));
    }

    @Test
    void failedEventIsNotDeletedSoSqsCanRetryAndRedriveIt() {
        SqsClient sqs = mock(SqsClient.class);
        SesEventService service = mock(SesEventService.class);
        Message message = message();
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        doThrow(new IllegalArgumentException("bad event"))
                .when(service).process(message.body());

        new SesEventQueueWorker(sqs, service, properties()).poll();

        verify(sqs, never()).deleteMessage(
                any(software.amazon.awssdk.services.sqs.model.DeleteMessageRequest.class));
    }

    private Message message() {
        return Message.builder()
                .messageId("sqs-1")
                .receiptHandle("receipt")
                .body("{}")
                .build();
    }

    private SesProperties properties() {
        SesProperties properties = new SesProperties();
        properties.setEventQueueUrl("queue-url");
        return properties;
    }
}
