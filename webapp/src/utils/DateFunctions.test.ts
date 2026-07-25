import { describe, expect, it } from "vitest";
import { formatDate, getOrdinal } from "./DateFunctions";

describe("date formatting", () => {
  it.each([
    [1, "st"],
    [2, "nd"],
    [3, "rd"],
    [4, "th"],
    [11, "th"],
    [12, "th"],
    [13, "th"],
    [21, "st"],
    [22, "nd"],
    [23, "rd"],
  ])("uses the correct ordinal for %i", (day, suffix) => {
    expect(getOrdinal(day)).toBe(suffix);
  });

  it("formats a date in a stable user-facing form", () => {
    expect(formatDate(new Date(2026, 6, 23))).toBe("July 23rd, 2026");
  });
});
