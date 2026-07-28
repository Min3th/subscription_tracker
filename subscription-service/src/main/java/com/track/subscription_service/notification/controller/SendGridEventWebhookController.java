package com.track.subscription_service.notification.controller;

import com.track.subscription_service.notification.service.SendGridEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/notifications/webhooks/sendgrid")
@ConditionalOnProperty(
        name = "app.email.sendgrid-inbound-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SendGridEventWebhookController {
    private static final String SIGNATURE_HEADER =
            "X-Twilio-Email-Event-Webhook-Signature";
    private static final String TIMESTAMP_HEADER =
            "X-Twilio-Email-Event-Webhook-Timestamp";

    private final SendGridEventService eventService;

    public SendGridEventWebhookController(SendGridEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(
            @RequestBody String payload,
            @RequestHeader(value = SIGNATURE_HEADER, required = false)
            String signature,
            @RequestHeader(value = TIMESTAMP_HEADER, required = false)
            String timestamp) {
        try {
            eventService.process(payload, signature, timestamp);
            return ResponseEntity.noContent().build();
        } catch (SecurityException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid webhook payload");
        }
    }
}
