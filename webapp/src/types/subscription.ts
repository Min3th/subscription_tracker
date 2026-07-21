export interface Subscription {
  id: number;
  name: string;
  category: SubscriptionCategory;
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
  nextBillingDate: Date | null;
  category: SubscriptionCategory;
  status: "active" | "cancelled" | "paused";
  paymentMethod: string;
  startDate: string;
  description: string;
  website: string;
  autoRenew: boolean;
  totalPaid: number;
  emailNotificationsEnabled: boolean;
}

export interface UpdateSubscriptionPayload {
  id: number;
  name?: string;
  description?: string;
  cost?: number;
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
