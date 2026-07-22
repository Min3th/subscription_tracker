export interface Subscription {
  id: number;
  name: string;
  category: string;
  cost: string;
  currency: string;
  type: SubscriptionType;
  billingIntervalCount: number;
  billingIntervalUnit: BillingUnit;
  startDate: string;
}

export interface DetailedSubscription {
  id: number;
  name: string;
  cost: string;
  currency: string;
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
  totalPaid: string;
  emailNotificationsEnabled: boolean;
}

export interface UpdateSubscriptionPayload {
  id: number;
  name?: string;
  description?: string;
  cost?: string;
  currency?: string;
  type?: SubscriptionType;
  category?: string;
  startDate?: string;
  paymentMethod?: string;
  website?: string;
  billingIntervalUnit?: BillingUnit;
  billingIntervalCount?: number;
  emailNotificationsEnabled?: boolean;
}

export type SubscriptionType = "one-time" | "recurring";
export type BillingUnit = "day" | "week" | "month" | "year";
