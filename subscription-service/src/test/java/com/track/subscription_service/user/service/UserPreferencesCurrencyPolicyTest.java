package com.track.subscription_service.user.service;

import com.track.subscription_service.notification.service.ReminderScheduleService;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.user.dto.UpdateUserPreferencesRequest;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.entity.UserPreferences;
import com.track.subscription_service.user.repository.UserPreferencesRepository;
import com.track.subscription_service.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserPreferencesCurrencyPolicyTest {

    private final UserPreferencesRepository preferences = mock(UserPreferencesRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final UserPreferencesService service = new UserPreferencesService(
            preferences, users, subscriptions, mock(ReminderScheduleService.class)
    );

    @Test
    void relabelsExistingSubscriptionsWithoutChangingTheirAmounts() {
        UserPreferences existing = existingPreferences("USD");
        var first = subscription(existing.getUser(), "10.00", "USD");
        var second = subscription(existing.getUser(), "25.50", "USD");
        when(users.findByGoogleId("google-id")).thenReturn(Optional.of(existing.getUser()));
        when(preferences.findByUser(existing.getUser())).thenReturn(Optional.of(existing));
        when(preferences.save(existing)).thenReturn(existing);
        when(subscriptions.findByUser_GoogleId("google-id")).thenReturn(List.of(first, second));

        UserPreferences saved = service.updatePreferences("google-id", update("LKR"));

        assertEquals("LKR", saved.getCurrency());
        assertEquals("LKR", first.getCurrency());
        assertEquals("LKR", second.getCurrency());
        assertEquals(new BigDecimal("10.00"), first.getCost());
        assertEquals(new BigDecimal("25.50"), second.getCost());
        verify(subscriptions).saveAll(List.of(first, second));
    }

    @Test
    void allowsOtherPreferenceChangesWhileSubscriptionsExist() {
        UserPreferences existing = existingPreferences("USD");
        when(users.findByGoogleId("google-id")).thenReturn(Optional.of(existing.getUser()));
        when(preferences.findByUser(existing.getUser())).thenReturn(Optional.of(existing));
        when(preferences.save(existing)).thenReturn(existing);
        when(subscriptions.findByUser_GoogleId("google-id")).thenReturn(List.of());

        UserPreferences saved = service.updatePreferences("google-id", update("USD"));

        assertEquals("USD", saved.getCurrency());
        assertEquals(true, saved.isOnboardingCompleted());
        verify(subscriptions, never()).saveAll(any());
        verify(preferences).save(existing);
    }

    @Test
    void newUsersStartWithOnboardingIncomplete() {
        User user = new User();
        user.setGoogleId("new-google-id");
        when(users.findByGoogleId("new-google-id")).thenReturn(Optional.of(user));
        when(preferences.findByUser(user)).thenReturn(Optional.empty());
        when(preferences.save(any(UserPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferences created = service.getByGoogleId("new-google-id");

        assertEquals(false, created.isOnboardingCompleted());
        verify(preferences).save(created);
    }

    private UserPreferences existingPreferences(String currency) {
        User user = new User();
        user.setGoogleId("google-id");
        UserPreferences existing = new UserPreferences(
                user, currency, "en", "UTC", "light", true, 3
        );
        user.setPreferences(existing);
        return existing;
    }

    private UpdateUserPreferencesRequest update(String currency) {
        return new UpdateUserPreferencesRequest(
                currency, "en", "UTC", "light", true, 3, LocalTime.of(9, 0), true
        );
    }

    private com.track.subscription_service.subscription.entity.Subscription subscription(
            User user, String cost, String currency
    ) {
        var subscription = new com.track.subscription_service.subscription.entity.Subscription();
        subscription.setUser(user);
        subscription.setCost(new BigDecimal(cost));
        subscription.setCurrency(currency);
        return subscription;
    }
}
