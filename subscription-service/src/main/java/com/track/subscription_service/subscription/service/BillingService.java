package com.track.subscription_service.subscription.service;

import org.springframework.stereotype.Service;
import com.track.subscription_service.subscription.model.BillingUnit;

import java.time.LocalDate;

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

    public double calculateTotalPaid(LocalDate startDate, BillingUnit unit, Integer count, double cost) {
        validateRecurringInputs(startDate, unit, count);
        if (cost <= 0) throw new IllegalArgumentException("Cost must be positive");
        if (startDate.isAfter(LocalDate.now())) return 0;

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

        return cycles * cost;
    }

    private void validateRecurringInputs(LocalDate startDate, BillingUnit unit, Integer count) {
        if (startDate == null) throw new IllegalArgumentException("Start date is required");
        if (unit == null) throw new IllegalArgumentException("Billing unit is required");
        if (count == null || count <= 0) throw new IllegalArgumentException("Billing interval count must be positive");
    }
}
