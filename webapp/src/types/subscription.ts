export interface Subscription {
  id: number;
  name: string;
  category: string;
  cost: number;
  type: SubscriptionType;
  billingIntervalCount: number;
  billingIntervalUnit: BillingUnit;
  startDate: string;
}

export interface DetailedSubscription {
  id: string;
  name: string;
  cost: number;
  billingIntervalUnit: BillingUnit;
  billingIntervalCount: number;
  nextBillingDate: Date;
  category: string;
  status: "active" | "cancelled" | "paused";
  paymentMethod: string;
  startDate: string;
  description: string;
  website: string;
  autoRenew: boolean;
  totalPaid: number;
}

export type SubscriptionType = "one-time" | "recurring";
export type BillingUnit = "day" | "week" | "month" | "year";
