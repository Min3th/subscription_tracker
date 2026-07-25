import { describe, expect, it } from "vitest";
import {
  divideRounded,
  formatDecimal,
  formatMoney,
  parseDecimal,
  toChartNumber,
} from "./money";

describe("money utilities", () => {
  it.each([
    ["0", 0n],
    ["12", 120000n],
    ["12.3", 123000n],
    ["12.3456", 123456n],
    [" 99.99 ", 999900n],
  ])("parses %s without floating-point arithmetic", (input, expected) => {
    expect(parseDecimal(input)).toBe(expected);
  });

  it.each(["-1", "1.23456", "1e3", "NaN", "", "12."])(
    "rejects invalid monetary input %s",
    (input) => {
      expect(() => parseDecimal(input)).toThrow("Invalid monetary value");
    },
  );

  it("rounds integer division to the nearest scaled unit", () => {
    expect(divideRounded(10n, 3n)).toBe(3n);
    expect(divideRounded(11n, 3n)).toBe(4n);
    expect(() => divideRounded(10n, 0n)).toThrow("Money divisor must be positive");
  });

  it("formats exact values and only converts to number at the chart boundary", () => {
    expect(formatDecimal(123456n)).toBe("12.34");
    expect(formatDecimal(-123456n, 4)).toBe("-12.3456");
    expect(formatMoney("19.9999", "LKR")).toBe("LKR 19.99");
    expect(toChartNumber(123456n)).toBe(12.3456);
  });
});
