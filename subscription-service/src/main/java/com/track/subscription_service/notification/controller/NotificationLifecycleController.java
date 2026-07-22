package com.track.subscription_service.notification.controller;

import com.track.subscription_service.notification.service.SendGridEventService;
import com.track.subscription_service.notification.service.UnsubscribeService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationLifecycleController {
    private static final String SIGNATURE_HEADER = "X-Twilio-Email-Event-Webhook-Signature";
    private static final String TIMESTAMP_HEADER = "X-Twilio-Email-Event-Webhook-Timestamp";
    private final UnsubscribeService unsubscribeService;
    private final SendGridEventService eventService;

    public NotificationLifecycleController(UnsubscribeService unsubscribeService,
                                           SendGridEventService eventService) {
        this.unsubscribeService = unsubscribeService;
        this.eventService = eventService;
    }

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmUnsubscribe(@RequestParam String token) {
        String safeToken = token.replaceAll("[^A-Za-z0-9_-]", "");
        String page = """
                <!doctype html><html><body><h1>Unsubscribe from reminders?</h1>
                <form method="post" action="/notifications/unsubscribe?token=%s">
                <button type="submit">Unsubscribe</button></form></body></html>
                """.formatted(safeToken);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestParam String token) {
        try {
            unsubscribeService.unsubscribe(token);
            return ResponseEntity.ok(Map.of("status", "unsubscribed"));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping(value = "/webhooks/sendgrid", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendGridEvents(
            @RequestBody String payload,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = TIMESTAMP_HEADER, required = false) String timestamp) {
        try {
            eventService.process(payload, signature, timestamp);
            return ResponseEntity.noContent().build();
        } catch (SecurityException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload");
        }
    }
}
