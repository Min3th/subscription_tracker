package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.SesInboundObject;
import com.track.subscription_service.notification.config.SesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesInboundQueueWorkerTest {
    private SqsClient sqs;
    private S3Client s3;
    private SesInboundNotificationParser parser;
    private InboundEmailIngestionService ingestion;
    private SesInboundQueueWorker worker;
    private Message message;

    @BeforeEach
    void setUp() {
        sqs = mock(SqsClient.class);
        s3 = mock(S3Client.class);
        parser = mock(SesInboundNotificationParser.class);
        ingestion = mock(InboundEmailIngestionService.class);
        InboundEmailProperties inboundProperties = new InboundEmailProperties();
        inboundProperties.setMaxRequestBytes(10 * 1024 * 1024);
        SesProperties sesProperties = new SesProperties();
        sesProperties.setInboundQueueUrl("inbound-queue");
        sesProperties.setInboundBucket("inbound-bucket");
        worker = new SesInboundQueueWorker(
                sqs, s3, parser, ingestion, inboundProperties, sesProperties);
        message = Message.builder()
                .messageId("sqs-inbound-1")
                .receiptHandle("receipt-handle")
                .body("{}")
                .build();
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
    }

    @Test
    void downloadsIngestsAndDeletesOnlyAfterSuccess() {
        byte[] rawMime = "Subject: Receipt\r\n\r\nPaid".getBytes();
        when(parser.parse(message.body())).thenReturn(metadata("inbound-bucket"));
        when(s3.getObject(any(GetObjectRequest.class)))
                .thenReturn(stream(rawMime, rawMime.length));

        worker.poll();

        verify(ingestion).ingest(
                org.mockito.ArgumentMatchers.argThat(email ->
                        "ses-inbound-1".equals(email.providerMessageId())
                                && java.util.Arrays.equals(rawMime, email.rawMime())));
        verify(sqs).deleteMessage(
                org.mockito.ArgumentMatchers.<DeleteMessageRequest>argThat(request ->
                        "inbound-queue".equals(request.queueUrl())
                                && "receipt-handle".equals(request.receiptHandle())));
    }

    @Test
    void unexpectedBucketIsNeitherDownloadedNorDeleted() {
        when(parser.parse(message.body())).thenReturn(metadata("attacker-bucket"));

        worker.poll();

        verify(s3, never()).getObject(any(GetObjectRequest.class));
        verify(ingestion, never()).ingest(any());
        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void oversizedObjectIsNotIngestedOrDeleted() {
        when(parser.parse(message.body())).thenReturn(metadata("inbound-bucket"));
        when(s3.getObject(any(GetObjectRequest.class)))
                .thenReturn(stream(new byte[0], 10L * 1024 * 1024 + 1));

        worker.poll();

        verify(ingestion, never()).ingest(any());
        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    private SesInboundObject metadata(String bucket) {
        return new SesInboundObject(
                "ses-inbound-1",
                "sender@example.com",
                List.of("sub-token@inbound.subtrak.me"),
                "PASS",
                "PASS",
                Instant.parse("2026-07-28T04:00:00Z"),
                bucket,
                "incoming/ses-inbound-1");
    }

    private ResponseInputStream<GetObjectResponse> stream(byte[] content, long length) {
        return new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(length).build(),
                AbortableInputStream.create(
                        new java.io.ByteArrayInputStream(content)));
    }
}
