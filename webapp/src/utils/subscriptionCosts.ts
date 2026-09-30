import type { DetailedSubscription } from "../types/subscription";
import { divideRounded, parseDecimal } from "./money";

type CostSubscription = Pick<DetailedSubscription,
  "cost" | "type" | "status" | "billingIntervalUnit" | "billingIntervalCount">;

// Normalized recurring estimates use a 365-day year, not actual calendar charges.
// Calculate each period directly so annual costs do not inherit monthly rounding.
export const getRecurringCost = (sub: CostSubscription, period: "month" | "year"): bigint => {
  if (sub.type !== "recurring" || sub.status !== "active") return 0n;
  if (!Number.isSafeInteger(sub.billingIntervalCount) || sub.billingIntervalCount <= 0) return 0n;

  const amount = parseDecimal(sub.cost);
  const count = BigInt(sub.billingIntervalCount);
  const months = period === "month" ? 12n : 1n;
  switch (sub.billingIntervalUnit) {
    case "day": return divideRounded(amount * 365n, count * months);
    case "week": return divideRounded(amount * 365n, count * 7n * months);
    case "month": return divideRounded(amount * 12n, count * months);
    case "year": return divideRounded(amount, count * months);
    default: return 0n;
  }
};
