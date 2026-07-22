package com.track.subscription_service.subscription.service;

import org.springframework.stereotype.Service;
import com.track.subscription_service.subscription.model.BillingUnit;

import java.time.LocalDate;
import java.math.BigDecimal;

@Service
public class BillingService {

    public LocalDate getNextBillingDate(LocalDate startDate, BillingUnit unit, Integer count) {
        validateRecurringInputs(startDate, unit, count);

        LocalDate now = LocalDate.now();

        if (startDate.isAfter(now)) {
            return startDate;
        }

        LocalDate next = startDate;

        while (!next.isAfter(now)) {
            switch (unit) {
                case DAY -> next = next.plusDays(count);
                case WEEK -> next = next.plusWeeks(count);
                case MONTH -> next = next.plusMonths(count);
                case YEAR -> next = next.plusYears(count);
            }
        }

        return next;
    }

    public LocalDate getBillingDateOnOrAfter(LocalDate startDate, BillingUnit unit,
                                             Integer count, LocalDate onOrAfter) {
        validateRecurringInputs(startDate, unit, count);
        if (onOrAfter == null) throw new IllegalArgumentException("Reference date is required");

        LocalDate billingDate = startDate;
        while (billingDate.isBefore(onOrAfter)) {
            billingDate = switch (unit) {
                case DAY -> billingDate.plusDays(count);
                case WEEK -> billingDate.plusWeeks(count);
                case MONTH -> billingDate.plusMonths(count);
                case YEAR -> billingDate.plusYears(count);
            };
        }
        return billingDate;
    }

    public BigDecimal calculateTotalPaid(LocalDate startDate, BillingUnit unit, Integer count, BigDecimal cost) {
        validateRecurringInputs(startDate, unit, count);
        if (cost == null || cost.signum() <= 0) throw new IllegalArgumentException("Cost must be positive");
        if (startDate.isAfter(LocalDate.now())) return BigDecimal.ZERO;

        long cycles = 0;
        LocalDate current = startDate;
        LocalDate now = LocalDate.now();

        while (current.isBefore(now) || current.isEqual(now)) {
            cycles++;

            current = switch (unit) {
                case DAY -> current.plusDays(count);
                case WEEK -> current.plusWeeks(count);
                case MONTH -> current.plusMonths(count);
                case YEAR -> current.plusYears(count);
            };
        }

        return cost.multiply(BigDecimal.valueOf(cycles));
    }
    private void validateRecurringInputs(LocalDate startDate, BillingUnit unit, Integer count) {
        if (startDate == null) throw new IllegalArgumentException("Start date is required");
        if (unit == null) throw new IllegalArgumentException("Billing unit is required");
        if (count == null || count <= 0) throw new IllegalArgumentException("Billing interval count must be positive");
    }
}
