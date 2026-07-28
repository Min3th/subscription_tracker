package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.SubscriptionExtraction;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class SubscriptionDuplicateMatcher {
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionDuplicateMatcher(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Optional<Subscription> findPossibleDuplicate(
            Long userId, SubscriptionExtraction extraction) {
        if (!hasExplicitBillingDetail(extraction)) {
            return Optional.empty();
        }
        String provider = normalizeName(extraction.provider());
        return subscriptionRepository.findByUser_Id(userId).stream()
                .filter(subscription -> providerMatches(provider, subscription.getName()))
                .filter(subscription -> billingDetailMatches(extraction, subscription))
                .findFirst();
    }

    private boolean hasExplicitBillingDetail(SubscriptionExtraction extraction) {
        return extraction.amount() != null || extraction.billingIntervalUnit() != null;
    }

    private boolean providerMatches(String provider, String subscriptionName) {
        String name = normalizeName(subscriptionName);
        return name.equals(provider) || name.startsWith(provider + " ");
    }

    private boolean billingDetailMatches(
            SubscriptionExtraction extraction, Subscription subscription) {
        boolean moneyMatches = extraction.amount() != null
                && extraction.amount().compareTo(subscription.getCost()) == 0
                && extraction.currency().equals(subscription.getCurrency());
        boolean cadenceMatches = extraction.billingIntervalUnit() != null
                && extraction.billingIntervalUnit() == subscription.getBillingIntervalUnit()
                && extraction.billingIntervalCount().equals(
                        subscription.getBillingIntervalCount());
        return moneyMatches || cadenceMatches;
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
