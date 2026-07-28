package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.NormalizedInboundEmail;
import com.track.subscription_service.inboundemail.dto.SubscriptionExtraction;
import com.track.subscription_service.inboundemail.entity.InboundEmail;
import com.track.subscription_service.inboundemail.entity.SubscriptionSuggestion;
import com.track.subscription_service.inboundemail.model.InboundEmailClassification;
import com.track.subscription_service.inboundemail.model.InboundEmailStatus;
import com.track.subscription_service.inboundemail.model.SuggestionEventType;
import com.track.subscription_service.inboundemail.model.SuggestionStatus;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import com.track.subscription_service.inboundemail.repository.SubscriptionSuggestionRepository;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InboundEmailProcessorTest {
    private static final Instant NOW = Instant.parse("2026-07-28T04:00:00Z");

    @Test
    void createsPendingSuggestionAndRecordsPossibleDuplicate() {
        InboundEmailRepository emails = mock(InboundEmailRepository.class);
        SubscriptionSuggestionRepository suggestions =
                mock(SubscriptionSuggestionRepository.class);
        InboundEmailNormalizer normalizer = mock(InboundEmailNormalizer.class);
        DeterministicSubscriptionExtractor extractor =
                mock(DeterministicSubscriptionExtractor.class);
        SubscriptionDuplicateMatcher duplicateMatcher =
                mock(SubscriptionDuplicateMatcher.class);
        UUID emailId = UUID.randomUUID();
        InboundEmail email = mock(InboundEmail.class);
        User user = mock(User.class);
        Subscription duplicate = new Subscription();
        NormalizedInboundEmail normalized =
                new NormalizedInboundEmail("Renewal", "netflix.com", "Paid");
        SubscriptionExtraction extraction = new SubscriptionExtraction(
                InboundEmailClassification.RENEWAL_PAYMENT,
                "Netflix",
                "Premium",
                new BigDecimal("22.9900"),
                "USD",
                BillingUnit.MONTH,
                1,
                LocalDate.of(2026, 8, 28),
                new BigDecimal("0.9500"),
                "provider,event,money,cadence,date");
        when(email.getId()).thenReturn(emailId);
        when(email.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(7L);
        when(normalizer.normalize(email)).thenReturn(normalized);
        when(extractor.extract(normalized)).thenReturn(extraction);
        when(duplicateMatcher.findPossibleDuplicate(7L, extraction))
                .thenReturn(Optional.of(duplicate));
        InboundEmailProcessor processor = new InboundEmailProcessor(
                emails, suggestions, normalizer, extractor, duplicateMatcher,
                Clock.fixed(NOW, ZoneOffset.UTC));

        processor.process(email, "claim");

        ArgumentCaptor<SubscriptionSuggestion> captor =
                ArgumentCaptor.forClass(SubscriptionSuggestion.class);
        verify(suggestions).save(captor.capture());
        SubscriptionSuggestion saved = captor.getValue();
        assertEquals(SuggestionStatus.PENDING, saved.getStatus());
        assertEquals(SuggestionEventType.RENEWAL_PAYMENT, saved.getEventType());
        assertEquals(extraction.actionUrl(), saved.getActionUrl());
        assertEquals(duplicate, saved.getPossibleDuplicateSubscription());
        verify(emails).markCompleted(emailId, "claim",
                InboundEmailStatus.SUGGESTION_CREATED, NOW);
    }

    @Test
    void ignoresNonSubscriptionWithoutCreatingSuggestion() {
        InboundEmailRepository emails = mock(InboundEmailRepository.class);
        SubscriptionSuggestionRepository suggestions =
                mock(SubscriptionSuggestionRepository.class);
        InboundEmailNormalizer normalizer = mock(InboundEmailNormalizer.class);
        DeterministicSubscriptionExtractor extractor =
                mock(DeterministicSubscriptionExtractor.class);
        InboundEmail email = mock(InboundEmail.class);
        UUID emailId = UUID.randomUUID();
        NormalizedInboundEmail normalized =
                new NormalizedInboundEmail("Receipt", "store.test", "Purchase");
        SubscriptionExtraction extraction = new SubscriptionExtraction(
                InboundEmailClassification.NOT_SUBSCRIPTION,
                null, null, null, null, null, null, null,
                BigDecimal.ZERO, "none");
        when(email.getId()).thenReturn(emailId);
        when(normalizer.normalize(email)).thenReturn(normalized);
        when(extractor.extract(normalized)).thenReturn(extraction);
        InboundEmailProcessor processor = new InboundEmailProcessor(
                emails, suggestions, normalizer, extractor,
                mock(SubscriptionDuplicateMatcher.class),
                Clock.fixed(NOW, ZoneOffset.UTC));

        processor.process(email, "claim");

        verify(suggestions, never()).save(any());
        verify(emails).markCompleted(emailId, "claim",
                InboundEmailStatus.IGNORED, NOW);
    }
}
