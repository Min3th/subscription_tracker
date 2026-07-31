package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.config.SesProperties;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
@ConditionalOnProperty(name = "app.email.outbound-provider", havingValue = "ses")
public class SesOutboundEmailSender implements OutboundEmailSender {
    private final SesV2Client client;
    private final SesProperties properties;

    public SesOutboundEmailSender(SesV2Client client, SesProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public String send(OutboundEmailMessage message) {
        try {
            SendEmailResponse response = client.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(formattedSender())
                    .destination(Destination.builder()
                            .toAddresses(message.to())
                            .build())
                    .configurationSetName(properties.getConfigurationSet())
                    .content(EmailContent.builder()
                            .raw(RawMessage.builder()
                                    .data(SdkBytes.fromByteArray(rawMessage(message)))
                                    .build())
                            .build())
                    .build());
            return response.messageId();
        } catch (SesV2Exception exception) {
            int statusCode = exception.statusCode();
            boolean retryable = statusCode == 429 || statusCode >= 500
                    || exception.retryable();
            throw EmailDeliveryException.providerRejected(statusCode, retryable);
        } catch (SdkClientException exception) {
            throw EmailDeliveryException.transportFailure(exception);
        } catch (Exception exception) {
            throw EmailDeliveryException.messageConstructionFailure(exception);
        }
    }

    private byte[] rawMessage(OutboundEmailMessage message) throws Exception {
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        mime.setFrom(new InternetAddress(
                properties.getFromEmail(),
                properties.getFromName(),
                StandardCharsets.UTF_8.name()));
        mime.setRecipient(Message.RecipientType.TO, new InternetAddress(message.to()));
        mime.setSubject(message.subject(), StandardCharsets.UTF_8.name());
        mime.setContent(message.html(), "text/html; charset=UTF-8");
        mime.setHeader("List-Unsubscribe", "<" + message.unsubscribeUrl() + ">");
        mime.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
        mime.saveChanges();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            mime.writeTo(output);
            return output.toByteArray();
        }
    }

    private String formattedSender() {
        try {
            return new InternetAddress(
                    properties.getFromEmail(),
                    properties.getFromName(),
                    StandardCharsets.UTF_8.name()).toUnicodeString();
        } catch (Exception exception) {
            throw EmailDeliveryException.messageConstructionFailure(exception);
        }
    }
}
