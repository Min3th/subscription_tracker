package com.track.subscription_service.subscription.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.track.subscription_service.subscription.model.BillingUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingServiceMoneyTest {

    @Test
    void totalPaidUsesExactDecimalArithmetic() {
        BillingService service = new BillingService();

        BigDecimal total = service.calculateTotalPaid(
                LocalDate.now().minusMonths(2), BillingUnit.MONTH, 1, new BigDecimal("0.10")
        );

        assertEquals(0, new BigDecimal("0.30").compareTo(total));
    }
}
