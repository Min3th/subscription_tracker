package com.track.subscription_service.subscription.service;

import com.track.subscription_service.notification.service.ReminderScheduleService;
import com.track.subscription_service.subscription.dto.CreateSubscriptionRequest;
import com.track.subscription_service.subscription.dto.UpdateSubscriptionRequest;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.model.SubscriptionCategory;
import com.track.subscription_service.subscription.model.SubscriptionType;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.entity.UserPreferences;
import com.track.subscription_service.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionCurrencyPolicyTest {

    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ReminderScheduleService reminders = mock(ReminderScheduleService.class);
    private final SubscriptionService service =
            new SubscriptionService(subscriptions, users, mock(BillingService.class), reminders);

    @Test
    void createUsesPreferredCurrencyInsteadOfRequestCurrency() {
        User user = userWithCurrency("LKR");
        when(users.findByGoogleId("google-id")).thenReturn(Optional.of(user));
        when(subscriptions.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription created = service.create(createRequest("USD"), "google-id");

        assertEquals("LKR", created.getCurrency());
    }

    @Test
    void updatePreservesExistingCurrency() {
        User user = userWithCurrency("LKR");
        Subscription existing = new Subscription();
        existing.setId(1L);
        existing.setUser(user);
        existing.setCurrency("USD");
        when(subscriptions.findByIdAndUser_GoogleId(1L, "google-id")).thenReturn(Optional.of(existing));
        when(subscriptions.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription updated = service.update(1L, updateRequest("LKR"), "google-id");

        assertEquals("USD", updated.getCurrency());
    }

    private User userWithCurrency(String currency) {
        User user = new User();
        user.setGoogleId("google-id");
        UserPreferences preferences = new UserPreferences();
        preferences.setCurrency(currency);
        preferences.setUser(user);
        user.setPreferences(preferences);
        return user;
    }

    private CreateSubscriptionRequest createRequest(String currency) {
        return new CreateSubscriptionRequest(
                "Example", new BigDecimal("5000"), currency, SubscriptionType.RECURRING,
                null, SubscriptionCategory.SOFTWARE, null, null, null,
                LocalDate.of(2026, 7, 26), BillingUnit.MONTH, 1, false
        );
    }

    private UpdateSubscriptionRequest updateRequest(String currency) {
        return new UpdateSubscriptionRequest(
                "Example", new BigDecimal("5000"), currency, SubscriptionType.RECURRING,
                null, SubscriptionCategory.SOFTWARE, null, null, null,
                LocalDate.of(2026, 7, 26), BillingUnit.MONTH, 1, false
        );
    }
}
