export interface Subscription {
  id: number;
  name: string;
  category: SubscriptionCategory;
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
  nextBillingDate: Date | null;
  category: SubscriptionCategory;
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
  category?: SubscriptionCategory;
  startDate?: string;
  paymentMethod?: string;
  website?: string;
  billingIntervalUnit?: BillingUnit;
  billingIntervalCount?: number;
  emailNotificationsEnabled?: boolean;
}

export type SubscriptionType = "one-time" | "recurring";
export type BillingUnit = "day" | "week" | "month" | "year";
export type SubscriptionCategory =
  | "General"
  | "Entertainment"
  | "Productivity"
  | "Music"
  | "Software"
  | "Utilities"
  | "Education"
  | "Health"
  | "Fitness"
  | "News"
  | "Cloud Storage"
  | "Finance"
  | "Other";

export const SUBSCRIPTION_CATEGORIES: SubscriptionCategory[] = [
  "General", "Entertainment", "Productivity", "Music", "Software", "Utilities",
  "Education", "Health", "Fitness", "News", "Cloud Storage", "Finance", "Other",
];
