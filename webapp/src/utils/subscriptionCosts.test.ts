import { describe, expect, it } from "vitest";
import type { BillingUnit } from "../types/subscription";
import { formatDecimal } from "./money";
import { getRecurringCost } from "./subscriptionCosts";

const subscription = (cost: string, billingIntervalUnit: BillingUnit, billingIntervalCount = 1) => ({
  cost, billingIntervalUnit, billingIntervalCount,
  type: "recurring" as const, status: "active" as const,
});

describe("normalized subscription costs", () => {
  it.each([
    ["4000", "day", 1, "121666.67", "1460000.00"],
    ["700", "week", 1, "3041.67", "36500.00"],
    ["1400", "week", 2, "3041.67", "36500.00"],
    ["300", "month", 3, "100.00", "1200.00"],
    ["100", "year", 1, "8.33", "100.00"],
    ["2400", "year", 2, "100.00", "1200.00"],
  ] as const)("normalizes %s every %s (%s)", (cost, unit, count, monthly, annual) => {
    const sub = subscription(cost, unit, count);
    expect(formatDecimal(getRecurringCost(sub, "month"))).toBe(monthly);
    expect(formatDecimal(getRecurringCost(sub, "year"))).toBe(annual);
  });

  it("preserves the annual price without multiplying rounded monthly amounts", () => {
    expect(getRecurringCost(subscription("100", "year"), "year")).toBe(1000000n);
  });

  it("excludes one-time and inactive subscriptions", () => {
    const sub = subscription("100", "month");
    for (const period of ["month", "year"] as const) {
      expect(getRecurringCost({ ...sub, type: "one-time" }, period)).toBe(0n);
      expect(getRecurringCost({ ...sub, status: "paused" }, period)).toBe(0n);
      expect(getRecurringCost({ ...sub, status: "cancelled" }, period)).toBe(0n);
    }
  });

  it.each([0, -1, 1.5, NaN, Infinity])("ignores invalid interval counts: %s", (count) => {
    expect(getRecurringCost(subscription("100", "month", count), "month")).toBe(0n);
  });
});
