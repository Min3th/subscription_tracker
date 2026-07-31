package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.NormalizedInboundEmail;
import com.track.subscription_service.inboundemail.dto.SubscriptionExtraction;
import com.track.subscription_service.inboundemail.model.InboundEmailClassification;
import com.track.subscription_service.subscription.model.BillingUnit;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicSubscriptionExtractorTest {
    private final DeterministicSubscriptionExtractor extractor =
            new DeterministicSubscriptionExtractor();

    @Test
    void extractsACompleteForwardedNetflixRenewalFromExplicitEvidence() throws IOException {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "Fwd: Your Netflix receipt",
                "gmail.com",
                fixture("netflix-renewal.txt")
        ));

        assertEquals(InboundEmailClassification.RENEWAL_PAYMENT, result.classification());
        assertEquals("Netflix", result.provider());
        assertEquals("Premium", result.planName());
        assertEquals(new BigDecimal("22.99"), result.amount());
        assertEquals("USD", result.currency());
        assertEquals(BillingUnit.MONTH, result.billingIntervalUnit());
        assertEquals(1, result.billingIntervalCount());
        assertEquals(LocalDate.of(2026, 8, 27), result.renewalDate());
        assertEquals(new BigDecimal("1.0000"), result.confidence());
        assertEquals(
                "provider-explicit,event-phrase,labeled-money,billing-cadence,"
                        + "explicit-renewal-date,labeled-plan",
                result.evidenceSummary());
    }

    @Test
    void extractsUnambiguousSymbolMoneyForANewSpotifySubscription() throws IOException {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "Welcome to Spotify",
                "spotify.com",
                fixture("spotify-new-subscription.txt")
        ));

        assertEquals(InboundEmailClassification.NEW_SUBSCRIPTION, result.classification());
        assertEquals("Spotify", result.provider());
        assertEquals(new BigDecimal("10.99"), result.amount());
        assertEquals("EUR", result.currency());
        assertEquals(BillingUnit.MONTH, result.billingIntervalUnit());
        assertTrue(result.isSuggestionCandidate());
    }

    @Test
    void extractsASanitizedChatGptPlusNewPlanReceipt() throws IOException {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "ChatGPT - Your new plan",
                "openai.com",
                fixture("chatgpt-plus-new-plan.txt")
        ));

        assertEquals(InboundEmailClassification.NEW_SUBSCRIPTION, result.classification());
        assertEquals("OpenAI", result.provider());
        assertEquals("ChatGPT Plus", result.planName());
        assertEquals(new BigDecimal("20.00"), result.amount());
        assertEquals("USD", result.currency());
        assertEquals(BillingUnit.MONTH, result.billingIntervalUnit());
        assertEquals(1, result.billingIntervalCount());
        assertTrue(result.isSuggestionCandidate());
    }

    @Test
    void extractsASanitizedNamecheapDomainRegistrationOrder() throws IOException {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "Namecheap order summary",
                "namecheap.com",
                fixture("namecheap-domain-registration.txt")
        ));

        assertEquals(InboundEmailClassification.NEW_SUBSCRIPTION, result.classification());
        assertEquals("Namecheap", result.provider());
        assertEquals("Domain Registration", result.planName());
        assertEquals(new BigDecimal("2.20"), result.amount());
        assertEquals("USD", result.currency());
        assertEquals(BillingUnit.YEAR, result.billingIntervalUnit());
        assertEquals(1, result.billingIntervalCount());
        assertTrue(result.isSuggestionCandidate());
    }

    @Test
    void doesNotTurnAnUnrelatedPurchaseOrAmbiguousDollarSymbolIntoASuggestion()
            throws IOException {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "Pickup reminder",
                "store.example",
                fixture("unrelated-purchase.txt")
        ));

        assertEquals(InboundEmailClassification.NOT_SUBSCRIPTION, result.classification());
        assertNull(result.provider());
        assertNull(result.amount());
        assertNull(result.currency());
        assertFalse(result.isSuggestionCandidate());
        assertEquals(BigDecimal.ZERO.setScale(4), result.confidence());
    }

    @Test
    void refusesToInferAYearForAnIncompleteRenewalDate() {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "Upcoming Netflix renewal",
                "netflix.com",
                "Your subscription will renew on August 27.\nPayment amount: USD 22.99"
        ));

        assertEquals(InboundEmailClassification.UPCOMING_RENEWAL, result.classification());
        assertNull(result.renewalDate());
        assertFalse(result.evidenceSummary().contains("explicit-renewal-date"));
    }

    @Test
    void extractsOnlyTheAllowlistedGmailForwardingVerificationUrl() {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "Gmail Forwarding Confirmation",
                "google.com",
                """
                Gmail received a forwarding confirmation request.
                Ignore https://evil.example/steal
                Confirm at https://mail-settings.google.com/mail/vf-abc123?token=safe
                """
        ));

        assertEquals(InboundEmailClassification.GMAIL_VERIFICATION, result.classification());
        assertEquals("https://mail-settings.google.com/mail/vf-abc123?token=safe",
                result.actionUrl());
        assertTrue(result.isSuggestionCandidate());
    }

    @Test
    void doesNotTrustGmailVerificationWordingFromAnotherSender() {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "Gmail Forwarding Confirmation",
                "attacker.example",
                "Gmail forwarding confirmation https://mail-settings.google.com/mail/vf-fake"
        ));

        assertEquals(InboundEmailClassification.NOT_SUBSCRIPTION, result.classification());
        assertNull(result.actionUrl());
        assertFalse(result.isSuggestionCandidate());
    }

    @Test
    void discardsLookalikeGoogleVerificationLinks() {
        SubscriptionExtraction result = extractor.extract(new NormalizedInboundEmail(
                "Gmail Forwarding Confirmation",
                "google.com",
                "Gmail forwarding confirmation https://mail-settings.google.com.evil.example/mail/vf-token"
        ));

        assertEquals(InboundEmailClassification.GMAIL_VERIFICATION, result.classification());
        assertNull(result.actionUrl());
    }

    private String fixture(String name) throws IOException {
        try (var input = getClass().getResourceAsStream("/inbound-email/" + name)) {
            if (input == null) {
                throw new IOException("Missing fixture " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
