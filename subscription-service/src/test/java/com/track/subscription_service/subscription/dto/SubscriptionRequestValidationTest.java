package com.track.subscription_service.subscription.dto;

import com.track.subscription_service.subscription.model.BillingUnit;
import com.track.subscription_service.subscription.model.SubscriptionCategory;
import com.track.subscription_service.subscription.model.SubscriptionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normalizesTextAndAcceptsAValidHttpUrl() {
        CreateSubscriptionRequest request = request("  Example  ", "  https://example.com/path  ", BillingUnit.MONTH, 1);

        assertEquals("Example", request.name());
        assertEquals("https://example.com/path", request.website());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidUrlAndMissingRecurringInterval() {
        assertFalse(validator.validate(request("Example", "javascript:alert(1)", BillingUnit.MONTH, 1)).isEmpty());
        assertFalse(validator.validate(request("Example", null, null, null)).isEmpty());
    }

    private CreateSubscriptionRequest request(String name, String website, BillingUnit unit, Integer count) {
        return new CreateSubscriptionRequest(
                name, new BigDecimal("10.0"), "USD", SubscriptionType.RECURRING, null, SubscriptionCategory.SOFTWARE,
                "  description  ", "  card  ", website, LocalDate.now(), unit, count, true
        );
    }
}
