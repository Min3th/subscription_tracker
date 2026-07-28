package com.track.subscription_service.inboundemail.controller;

import com.track.subscription_service.inboundemail.dto.SubscriptionSuggestionResponse;
import com.track.subscription_service.inboundemail.service.SubscriptionSuggestionService;
import com.track.subscription_service.subscription.dto.CreateSubscriptionRequest;
import com.track.subscription_service.subscription.dto.SubscriptionResponse;
import com.track.subscription_service.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inbound-email/suggestions")
public class SubscriptionSuggestionController {
    private final SubscriptionSuggestionService suggestionService;
    private final SubscriptionService subscriptionService;

    public SubscriptionSuggestionController(
            SubscriptionSuggestionService suggestionService,
            SubscriptionService subscriptionService) {
        this.suggestionService = suggestionService;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<SubscriptionSuggestionResponse> listPending(
            Authentication authentication) {
        return suggestionService.listPending(authentication.getName());
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<SubscriptionResponse> confirm(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSubscriptionRequest request,
            Authentication authentication) {
        var subscription =
                suggestionService.confirm(id, request, authentication.getName());
        return ResponseEntity
                .created(URI.create("/subscriptions/" + subscription.getId()))
                .body(subscriptionService.mapToResponse(subscription));
    }

    @PostMapping("/{id}/ignore")
    public ResponseEntity<Void> ignore(
            @PathVariable UUID id,
            Authentication authentication) {
        suggestionService.ignore(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/gmail-verification/complete")
    public ResponseEntity<Void> completeGmailVerification(
            @PathVariable UUID id,
            Authentication authentication) {
        suggestionService.completeGmailVerification(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
