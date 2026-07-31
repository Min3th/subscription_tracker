package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.SubscriptionExtraction;
import com.track.subscription_service.inboundemail.model.InboundEmailClassification;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionDuplicateMatcherTest {
    private final SubscriptionRepository repository = mock(SubscriptionRepository.class);
    private final SubscriptionDuplicateMatcher matcher =
            new SubscriptionDuplicateMatcher(repository);

    @Test
    void flagsMatchingProviderAndExactMoneyAsAPossibleDuplicate() {
        Subscription existing = subscription("Netflix Premium", "22.99", "USD");
        when(repository.findByUser_Id(7L)).thenReturn(List.of(existing));

        var match = matcher.findPossibleDuplicate(
                7L, extraction("Netflix", new BigDecimal("22.9900"), "USD"));

        assertEquals(existing, match.orElseThrow());
    }

    @Test
    void doesNotFlagProviderNameAlone() {
        when(repository.findByUser_Id(7L))
                .thenReturn(List.of(subscription("Netflix", "19.99", "USD")));

        var match = matcher.findPossibleDuplicate(
                7L, extraction("Netflix", null, null));

        assertTrue(match.isEmpty());
    }

    private Subscription subscription(String name, String cost, String currency) {
        Subscription subscription = new Subscription();
        subscription.setName(name);
        subscription.setCost(new BigDecimal(cost));
        subscription.setCurrency(currency);
        return subscription;
    }

    private SubscriptionExtraction extraction(
            String provider, BigDecimal amount, String currency) {
        return new SubscriptionExtraction(
                InboundEmailClassification.RENEWAL_PAYMENT,
                provider,
                null,
                amount,
                currency,
                null,
                null,
                null,
                new BigDecimal("0.8000"),
                "provider,event");
    }
}
