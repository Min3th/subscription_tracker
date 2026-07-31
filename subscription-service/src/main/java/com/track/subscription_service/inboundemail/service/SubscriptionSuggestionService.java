package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.common.error.ResourceNotFoundException;
import com.track.subscription_service.inboundemail.dto.SubscriptionSuggestionResponse;
import com.track.subscription_service.inboundemail.entity.SubscriptionSuggestion;
import com.track.subscription_service.inboundemail.model.SuggestionStatus;
import com.track.subscription_service.inboundemail.model.SuggestionEventType;
import com.track.subscription_service.inboundemail.repository.SubscriptionSuggestionRepository;
import com.track.subscription_service.subscription.dto.CreateSubscriptionRequest;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionSuggestionService {
    private final SubscriptionSuggestionRepository suggestionRepository;
    private final SubscriptionService subscriptionService;
    private final Clock clock;

    public SubscriptionSuggestionService(
            SubscriptionSuggestionRepository suggestionRepository,
            SubscriptionService subscriptionService,
            Clock clock) {
        this.suggestionRepository = suggestionRepository;
        this.subscriptionService = subscriptionService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionSuggestionResponse> listPending(String googleId) {
        return suggestionRepository
                .findByUser_GoogleIdAndStatusOrderByCreatedAtDesc(
                        googleId, SuggestionStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public Subscription confirm(
            UUID suggestionId,
            CreateSubscriptionRequest request,
            String googleId) {
        SubscriptionSuggestion suggestion = findOwnedForUpdate(suggestionId, googleId);
        requirePending(suggestion);
        if (suggestion.getEventType() == SuggestionEventType.GMAIL_VERIFICATION) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Gmail verification cannot create a subscription");
        }
        Subscription subscription =
                subscriptionService.createFromSuggestion(request, suggestion.getUser());
        var now = clock.instant();
        suggestion.setConfirmedSubscription(subscription);
        suggestion.setStatus(SuggestionStatus.CONFIRMED);
        suggestion.setDecidedAt(now);
        suggestion.setUpdatedAt(now);
        suggestionRepository.save(suggestion);
        return subscription;
    }

    @Transactional
    public void ignore(UUID suggestionId, String googleId) {
        SubscriptionSuggestion suggestion = findOwnedForUpdate(suggestionId, googleId);
        requirePending(suggestion);
        var now = clock.instant();
        suggestion.setStatus(SuggestionStatus.IGNORED);
        suggestion.setActionUrl(null);
        suggestion.setDecidedAt(now);
        suggestion.setUpdatedAt(now);
        suggestionRepository.save(suggestion);
    }

    @Transactional
    public void completeGmailVerification(UUID suggestionId, String googleId) {
        SubscriptionSuggestion suggestion = findOwnedForUpdate(suggestionId, googleId);
        requirePending(suggestion);
        if (suggestion.getEventType() != SuggestionEventType.GMAIL_VERIFICATION) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Suggestion is not a Gmail verification");
        }
        var now = clock.instant();
        suggestion.setStatus(SuggestionStatus.CONFIRMED);
        suggestion.setActionUrl(null);
        suggestion.setDecidedAt(now);
        suggestion.setUpdatedAt(now);
        suggestionRepository.save(suggestion);
    }

    private SubscriptionSuggestion findOwnedForUpdate(
            UUID suggestionId, String googleId) {
        return suggestionRepository.findByIdAndUser_GoogleId(suggestionId, googleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Subscription suggestion not found"));
    }

    private void requirePending(SubscriptionSuggestion suggestion) {
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subscription suggestion has already been decided");
        }
    }

    private SubscriptionSuggestionResponse toResponse(
            SubscriptionSuggestion suggestion) {
        var duplicate = suggestion.getPossibleDuplicateSubscription() == null
                ? null
                : new SubscriptionSuggestionResponse.PossibleDuplicate(
                        suggestion.getPossibleDuplicateSubscription().getId(),
                        suggestion.getPossibleDuplicateSubscription().getName());
        return new SubscriptionSuggestionResponse(
                suggestion.getId(),
                suggestion.getProvider(),
                suggestion.getPlanName(),
                suggestion.getAmount(),
                suggestion.getCurrency(),
                suggestion.getBillingIntervalUnit(),
                suggestion.getBillingIntervalCount(),
                suggestion.getStartDate(),
                suggestion.getRenewalDate(),
                suggestion.getEventType(),
                suggestion.getConfidence(),
                suggestion.getEvidenceSummary(),
                suggestion.getActionUrl(),
                suggestion.getStatus(),
                duplicate,
                suggestion.getInboundEmail().getReceivedAt(),
                suggestion.getCreatedAt());
    }
}
