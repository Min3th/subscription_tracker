export const MONEY_SCALE = 4;
const SCALE_FACTOR = 10n ** BigInt(MONEY_SCALE);

export const SUPPORTED_CURRENCIES = ["USD", "EUR", "GBP", "JPY", "CAD", "AUD", "INR", "LKR"] as const;
export type CurrencyCode = (typeof SUPPORTED_CURRENCIES)[number];

export const parseDecimal = (value: string): bigint => {
  const normalized = value.trim();
  if (!/^\d+(\.\d{1,4})?$/.test(normalized)) throw new Error(`Invalid monetary value: ${value}`);
  const [whole, fraction = ""] = normalized.split(".");
  return BigInt(whole) * SCALE_FACTOR + BigInt(fraction.padEnd(MONEY_SCALE, "0"));
};

export const divideRounded = (amount: bigint, divisor: bigint): bigint => {
  if (divisor <= 0n) throw new Error("Money divisor must be positive");
  return (amount + divisor / 2n) / divisor;
};

export const formatDecimal = (amount: bigint, fractionDigits = 2): string => {
  const negative = amount < 0n;
  const absolute = negative ? -amount : amount;
  const whole = absolute / SCALE_FACTOR;
  const fraction = (absolute % SCALE_FACTOR).toString().padStart(MONEY_SCALE, "0").slice(0, fractionDigits);
  return `${negative ? "-" : ""}${whole}.${fraction}`;
};

export const formatMoney = (value: string, currency: string): string =>
  `${currency} ${formatDecimal(parseDecimal(value))}`;

export const toChartNumber = (amount: bigint): number => Number(formatDecimal(amount, MONEY_SCALE));
