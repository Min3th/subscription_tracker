package com.track.subscription_service.subscription.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class BillingService {

    public LocalDate getNextBillingDate(LocalDate startDate, String unit, int count) {
        if (startDate == null || unit == null || count <= 0) {
            return LocalDate.now();
        }

        LocalDate now = LocalDate.now();

        if (startDate.isAfter(now)) {
            return startDate;
        }

        LocalDate next = startDate;

        while (!next.isAfter(now)) {
            switch (unit.toLowerCase()) {
                case "day" -> next = next.plusDays(count);
                case "week" -> next = next.plusWeeks(count);
                case "month" -> next = next.plusMonths(count);
                case "year" -> next = next.plusYears(count);
                default -> { return next; }
            }
        }

        return next;
    }

    public double calculateTotalPaid(LocalDate startDate, String unit, int count, double cost) {
        if (startDate.isAfter(LocalDate.now())) return 0;

        long cycles = 0;
        LocalDate current = startDate;
        LocalDate now = LocalDate.now();

        while (current.isBefore(now) || current.isEqual(now)) {
            cycles++;

            current = switch (unit.toLowerCase()) {
                case "day" -> current.plusDays(count);
                case "week" -> current.plusWeeks(count);
                case "month" -> current.plusMonths(count);
                case "year" -> current.plusYears(count);
                default -> current;
            };
        }

        return cycles * cost;
    }
}