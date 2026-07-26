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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserPreferencesCurrencyPolicyTest {

    private final UserPreferencesRepository preferences = mock(UserPreferencesRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final UserPreferencesService service = new UserPreferencesService(
            preferences, users, subscriptions, mock(ReminderScheduleService.class)
    );

    @Test
    void rejectsCurrencyChangeWhileSubscriptionsExist() {
        UserPreferences existing = existingPreferences("USD");
        when(users.findByGoogleId("google-id")).thenReturn(Optional.of(existing.getUser()));
        when(preferences.findByUser(existing.getUser())).thenReturn(Optional.of(existing));
        when(subscriptions.existsByUser_GoogleId("google-id")).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.updatePreferences("google-id", update("LKR"))
        );

        assertEquals(
                "Currency cannot be changed while subscriptions exist. Delete all subscriptions first.",
                error.getMessage()
        );
        assertEquals("USD", existing.getCurrency());
        verify(preferences, never()).save(any());
    }

    @Test
    void allowsOtherPreferenceChangesWhileSubscriptionsExist() {
        UserPreferences existing = existingPreferences("USD");
        when(users.findByGoogleId("google-id")).thenReturn(Optional.of(existing.getUser()));
        when(preferences.findByUser(existing.getUser())).thenReturn(Optional.of(existing));
        when(preferences.save(existing)).thenReturn(existing);

        UserPreferences saved = service.updatePreferences("google-id", update("USD"));

        assertEquals("USD", saved.getCurrency());
        assertEquals(true, saved.isOnboardingCompleted());
        verify(subscriptions, never()).existsByUser_GoogleId(anyString());
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
}
