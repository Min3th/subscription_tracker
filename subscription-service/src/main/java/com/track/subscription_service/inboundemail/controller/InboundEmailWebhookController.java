package com.track.subscription_service.inboundemail.controller;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.service.InboundEmailIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/webhooks/inbound-email")
@ConditionalOnProperty(
        name = "app.email.sendgrid-inbound-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InboundEmailWebhookController {
    private static final String SIGNATURE_HEADER =
            "X-Twilio-Email-Event-Webhook-Signature";
    private static final String TIMESTAMP_HEADER =
            "X-Twilio-Email-Event-Webhook-Timestamp";

    private final InboundEmailIngestionService ingestionService;
    private final InboundEmailProperties properties;

    public InboundEmailWebhookController(InboundEmailIngestionService ingestionService,
                                         InboundEmailProperties properties) {
        this.ingestionService = ingestionService;
        this.properties = properties;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> receive(
            HttpServletRequest request,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = TIMESTAMP_HEADER, required = false) String timestamp) {
        try {
            byte[] rawBody = readBoundedBody(request);
            ingestionService.receive(
                    rawBody,
                    request.getContentType(),
                    signature,
                    timestamp
            );
            return ResponseEntity.noContent().build();
        } catch (SecurityException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid inbound email payload");
        }
    }

    private byte[] readBoundedBody(HttpServletRequest request) {
        long contentLength = request.getContentLengthLong();
        if (contentLength > properties.getMaxRequestBytes()) {
            throw new IllegalArgumentException("Inbound email request exceeds the size limit");
        }
        try {
            byte[] body = request.getInputStream()
                    .readNBytes(Math.toIntExact(properties.getMaxRequestBytes()) + 1);
            if (body.length > properties.getMaxRequestBytes()) {
                throw new IllegalArgumentException("Inbound email request exceeds the size limit");
            }
            return body;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read inbound email request", exception);
        }
    }
}
