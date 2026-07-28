package com.track.subscription_service.integration;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.service.InboundEmailIngestionService;
import com.track.subscription_service.inboundemail.service.InboundEmailProcessingWorker;
import com.track.subscription_service.inboundemail.service.InboundEmailTokenCodec;
import com.track.subscription_service.inboundemail.service.SesInboundNotificationParser;
import com.track.subscription_service.inboundemail.service.SesInboundQueueWorker;
import com.track.subscription_service.notification.config.SesProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "google.client.id=test-google-client-id",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.inbound-email.processing-initial-delay-ms=3600000"
})
class SesInboundAdapterIntegrationTest extends PostgresIntegrationTest {
    private static final String GOOGLE_ID = "ses-inbound-adapter-integration";
    private static final String TOKEN = "s".repeat(43);
    private static final byte[] RAW_MIME = """
            From: Netflix Billing <billing@netflix.com>\r
            To: original-user@gmail.com\r
            Message-ID: <netflix-receipt-1@example.com>\r
            Subject: Your Netflix renewal receipt\r
            Content-Type: text/plain; charset=UTF-8\r
            \r
            Netflix renewal receipt\r
            Plan: Premium\r
            Payment received: USD 22.99\r
            Billing: monthly\r
            Next billing date: August 27, 2026\r
            """.getBytes(StandardCharsets.UTF_8);

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private InboundEmailTokenCodec tokenCodec;
    @Autowired
    private InboundEmailIngestionService ingestionService;
    @Autowired
    private SesInboundNotificationParser notificationParser;
    @Autowired
    private InboundEmailProperties inboundProperties;
    @Autowired
    private InboundEmailProcessingWorker processingWorker;

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id = ?", GOOGLE_ID);
    }

    @Test
    void duplicateAwsDeliveryCreatesOneLedgerRecordAndOneSuggestion() {
        long userId = jdbc.queryForObject("""
                INSERT INTO users (google_id, email, name)
                VALUES (?, 'ses-adapter@example.com', 'SES Adapter Test')
                RETURNING id
                """, Long.class, GOOGLE_ID);
        jdbc.update("""
                INSERT INTO inbound_email_address (
                    user_id, token_hash, encrypted_token, created_at
                ) VALUES (?, ?, 'encrypted-token', CURRENT_TIMESTAMP)
                """, userId, tokenCodec.hash(TOKEN));

        SqsClient sqs = mock(SqsClient.class);
        S3Client s3 = mock(S3Client.class);
        Message message = Message.builder()
                .messageId("sqs-duplicate")
                .receiptHandle("receipt")
                .body(notification())
                .build();
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(s3.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> objectStream());

        SesProperties ses = new SesProperties();
        ses.setInboundQueueUrl("inbound-queue");
        ses.setInboundBucket("inbound-bucket");
        SesInboundQueueWorker worker = new SesInboundQueueWorker(
                sqs, s3, notificationParser, ingestionService, inboundProperties, ses);

        worker.poll();
        worker.poll();
        processingWorker.processDueEmails();

        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM inbound_email WHERE user_id = ?",
                Integer.class, userId));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM subscription_suggestion
                WHERE user_id = ? AND status = 'PENDING'
                """, Integer.class, userId));
        verify(sqs, times(2)).deleteMessage(
                any(software.amazon.awssdk.services.sqs.model.DeleteMessageRequest.class));
    }

    private ResponseInputStream<GetObjectResponse> objectStream() {
        return new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength((long) RAW_MIME.length).build(),
                AbortableInputStream.create(new ByteArrayInputStream(RAW_MIME)));
    }

    private String notification() {
        return """
                {
                  "notificationType": "Received",
                  "mail": {
                    "timestamp": "2026-07-28T04:00:00Z",
                    "source": "forwarder@gmail.com",
                    "messageId": "ses-inbound-duplicate"
                  },
                  "receipt": {
                    "timestamp": "2026-07-28T04:00:01Z",
                    "recipients": ["sub-%s@inbound.subtrak.me"],
                    "spamVerdict": {"status": "PASS"},
                    "virusVerdict": {"status": "PASS"},
                    "action": {
                      "type": "S3",
                      "bucketName": "inbound-bucket",
                      "objectKey": "incoming/ses-inbound-duplicate"
                    }
                  }
                }
                """.formatted(TOKEN);
    }
}
