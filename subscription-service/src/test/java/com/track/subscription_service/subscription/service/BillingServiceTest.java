package com.track.subscription_service.subscription.service;

import com.track.subscription_service.subscription.model.BillingUnit;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BillingServiceTest {

    private final BillingService service = new BillingService();

    @Test
    void rejectsMissingOrNonPositiveRecurringIntervals() {
        LocalDate start = LocalDate.now().minusMonths(1);

        assertThrows(IllegalArgumentException.class, () -> service.getNextBillingDate(start, null, 1));
        assertThrows(IllegalArgumentException.class, () -> service.getNextBillingDate(start, BillingUnit.MONTH, null));
        assertThrows(IllegalArgumentException.class, () -> service.getNextBillingDate(start, BillingUnit.MONTH, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getNextBillingDate(start, BillingUnit.MONTH, -1));
    }

    @Test
    void computesAValidRecurringBillingDate() {
        LocalDate next = service.getNextBillingDate(LocalDate.now().minusMonths(2), BillingUnit.MONTH, 1);
        assertTrue(next.isAfter(LocalDate.now()));
    }
}
