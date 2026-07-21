package com.track.subscription_service.subscription.controller;

import com.track.subscription_service.subscription.dto.SubscriptionResponse;
import com.track.subscription_service.subscription.dto.UpdateSubscriptionRequest;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class SubscriptionControllerTest {

    @Test
    void updateUsesPathIdAndAuthenticatedUser() {
        SubscriptionService service = mock(SubscriptionService.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("google-user-123");

        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
                "Example",
                12.50,
                "recurring",
                null,
                "Software",
                "Example subscription",
                "Visa",
                "https://example.com",
                LocalDate.of(2026, 1, 1),
                "month",
                1,
                true
        );
        Subscription updated = new Subscription();
        SubscriptionResponse response = new SubscriptionResponse();
        when(service.update(42L, request, "google-user-123")).thenReturn(updated);
        when(service.mapToResponse(updated)).thenReturn(response);

        SubscriptionController controller = new SubscriptionController(service);
        var result = controller.updateSubscription(42L, request, authentication);

        assertSame(response, result.getBody());
        verify(service).update(42L, request, "google-user-123");
        verify(service).mapToResponse(updated);
        verifyNoMoreInteractions(service);
    }
}
