import type {
  BillingUnit,
  SubscriptionCategory,
  SubscriptionType,
} from "./subscription";

export type SuggestionEventType =
  | "NEW_SUBSCRIPTION"
  | "RENEWAL"
  | "PRICE_CHANGE"
  | "CANCELLATION"
  | "GMAIL_VERIFICATION";

export interface PossibleDuplicate {
  subscriptionId: number;
  name: string;
}

export interface SubscriptionSuggestion {
  id: string;
  provider: string;
  planName: string | null;
  amount: number | null;
  currency: string | null;
  billingIntervalUnit: BillingUnit | null;
  billingIntervalCount: number | null;
  renewalDate: string | null;
  eventType: SuggestionEventType;
  confidence: number;
  evidenceSummary: string;
  status: "PENDING";
  possibleDuplicate: PossibleDuplicate | null;
  receivedAt: string;
  createdAt: string;
}

export interface ConfirmSuggestionRequest {
  name: string;
  cost: number;
  currency: string;
  type: SubscriptionType;
  duration?: string | null;
  category: SubscriptionCategory;
  description?: string | null;
  paymentMethod?: string | null;
  website?: string | null;
  startDate: string;
  billingIntervalUnit?: BillingUnit | null;
  billingIntervalCount?: number | null;
  emailNotificationsEnabled: boolean;
}
