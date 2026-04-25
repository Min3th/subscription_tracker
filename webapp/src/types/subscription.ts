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
  id: number;
  name: string;
  cost: number;
  type: SubscriptionType;
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

export interface UpdateSubscriptionPayload {
  id: number;
  name?: string;
  description?: string;
  cost?: number;
  type?: SubscriptionType;
  category?: string;
  startDate?: string;
  paymentMethod?: string;
  website?: string;
  billingIntervalUnit?: BillingUnit;
  billingIntervalCount?: number;
}

export type SubscriptionType = "one-time" | "recurring";
export type BillingUnit = "day" | "week" | "month" | "year";
