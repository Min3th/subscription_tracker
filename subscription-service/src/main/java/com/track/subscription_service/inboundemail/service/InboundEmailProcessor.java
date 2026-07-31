package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.SubscriptionExtraction;
import com.track.subscription_service.inboundemail.entity.InboundEmail;
import com.track.subscription_service.inboundemail.entity.SubscriptionSuggestion;
import com.track.subscription_service.inboundemail.model.InboundEmailStatus;
import com.track.subscription_service.inboundemail.model.SuggestionEventType;
import com.track.subscription_service.inboundemail.model.SuggestionStatus;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import com.track.subscription_service.inboundemail.repository.SubscriptionSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class InboundEmailProcessor {
    private final InboundEmailRepository emailRepository;
    private final SubscriptionSuggestionRepository suggestionRepository;
    private final InboundEmailNormalizer normalizer;
    private final DeterministicSubscriptionExtractor extractor;
    private final SubscriptionDuplicateMatcher duplicateMatcher;
    private final Clock clock;

    public InboundEmailProcessor(
            InboundEmailRepository emailRepository,
            SubscriptionSuggestionRepository suggestionRepository,
            InboundEmailNormalizer normalizer,
            DeterministicSubscriptionExtractor extractor,
            SubscriptionDuplicateMatcher duplicateMatcher,
            Clock clock) {
        this.emailRepository = emailRepository;
        this.suggestionRepository = suggestionRepository;
        this.normalizer = normalizer;
        this.extractor = extractor;
        this.duplicateMatcher = duplicateMatcher;
        this.clock = clock;
    }

    @Transactional
    public void process(InboundEmail email, String claimToken) {
        if (suggestionRepository.existsByInboundEmailId(email.getId())) {
            emailRepository.markCompleted(email.getId(), claimToken,
                    InboundEmailStatus.SUGGESTION_CREATED, clock.instant());
            return;
        }

        var normalized = normalizer.normalize(email);
        SubscriptionExtraction extraction = extractor.extract(normalized);
        var completedAt = clock.instant();

        if (!extraction.isSuggestionCandidate()) {
            emailRepository.markCompleted(email.getId(), claimToken,
                    InboundEmailStatus.IGNORED, completedAt);
            return;
        }

        SubscriptionSuggestion suggestion = new SubscriptionSuggestion();
        suggestion.setUser(email.getUser());
        suggestion.setInboundEmail(email);
        suggestion.setProvider(extraction.provider());
        suggestion.setPlanName(extraction.planName());
        suggestion.setAmount(extraction.amount());
        suggestion.setCurrency(extraction.currency());
        suggestion.setBillingIntervalUnit(extraction.billingIntervalUnit());
        suggestion.setBillingIntervalCount(extraction.billingIntervalCount());
        suggestion.setStartDate(extraction.startDate());
        suggestion.setRenewalDate(extraction.renewalDate());
        suggestion.setEventType(SuggestionEventType.valueOf(
                extraction.classification().name()));
        suggestion.setConfidence(extraction.confidence());
        suggestion.setEvidenceSummary(extraction.evidenceSummary());
        suggestion.setActionUrl(extraction.actionUrl());
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setCreatedAt(completedAt);
        suggestion.setUpdatedAt(completedAt);
        duplicateMatcher.findPossibleDuplicate(email.getUser().getId(), extraction)
                .ifPresent(suggestion::setPossibleDuplicateSubscription);
        suggestionRepository.save(suggestion);

        emailRepository.markCompleted(email.getId(), claimToken,
                InboundEmailStatus.SUGGESTION_CREATED, completedAt);
    }
}
