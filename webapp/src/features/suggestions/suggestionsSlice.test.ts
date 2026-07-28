import { configureStore } from "@reduxjs/toolkit";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  confirmSuggestion as confirmSuggestionRequest,
  completeGmailVerification as completeGmailVerificationRequest,
  getPendingSuggestions,
  ignoreSuggestion as ignoreSuggestionRequest,
} from "../../api/inboundEmail";
import suggestionsReducer, {
  confirmSuggestion,
  completeGmailVerification,
  fetchSuggestions,
  ignoreSuggestion,
} from "./suggestionsSlice";
import type { SubscriptionSuggestion } from "../../types/suggestion";

vi.mock("../../api/inboundEmail", () => ({
  getPendingSuggestions: vi.fn(),
  confirmSuggestion: vi.fn(),
  completeGmailVerification: vi.fn(),
  ignoreSuggestion: vi.fn(),
}));

const suggestion: SubscriptionSuggestion = {
  id: "fdc2cf1a-342d-44b1-9421-0429925785db",
  provider: "Example",
  planName: "Plus",
  amount: 12,
  currency: "USD",
  billingIntervalUnit: "month",
  billingIntervalCount: 1,
  renewalDate: "2026-08-01",
  eventType: "NEW_SUBSCRIPTION",
  confidence: 0.93,
  evidenceSummary: "Example Plus renews monthly.",
  actionUrl: null,
  status: "PENDING",
  possibleDuplicate: null,
  receivedAt: "2026-07-28T10:00:00Z",
  createdAt: "2026-07-28T10:01:00Z",
};

const createStore = () =>
  configureStore({ reducer: { suggestions: suggestionsReducer } });

describe("suggestion request coordination", () => {
  beforeEach(() => vi.clearAllMocks());

  it("loads suggestions and deduplicates concurrent fetches", async () => {
    let resolveRequest!: (value: SubscriptionSuggestion[]) => void;
    vi.mocked(getPendingSuggestions).mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve;
      }),
    );
    const store = createStore();

    const first = store.dispatch(fetchSuggestions());
    const duplicate = store.dispatch(fetchSuggestions());
    resolveRequest([suggestion]);
    await Promise.all([first, duplicate]);

    expect(getPendingSuggestions).toHaveBeenCalledOnce();
    expect(store.getState().suggestions.items).toEqual([suggestion]);
  });

  it("removes a suggestion only after confirmation succeeds", async () => {
    vi.mocked(getPendingSuggestions).mockResolvedValue([suggestion]);
    vi.mocked(confirmSuggestionRequest).mockResolvedValue({ id: 42 } as never);
    const store = createStore();
    await store.dispatch(fetchSuggestions());

    const request = {
      name: "Example Plus",
      cost: 12,
      currency: "USD",
      type: "recurring" as const,
      category: "Software" as const,
      startDate: "2026-07-28",
      billingIntervalUnit: "month" as const,
      billingIntervalCount: 1,
      emailNotificationsEnabled: true,
    };
    await store.dispatch(confirmSuggestion({ id: suggestion.id, request }));

    expect(confirmSuggestionRequest).toHaveBeenCalledWith(suggestion.id, request);
    expect(store.getState().suggestions.items).toEqual([]);
  });

  it("keeps a suggestion when ignoring it fails", async () => {
    vi.mocked(getPendingSuggestions).mockResolvedValue([suggestion]);
    vi.mocked(ignoreSuggestionRequest).mockRejectedValue(new Error("Try again"));
    const store = createStore();
    await store.dispatch(fetchSuggestions());

    await store.dispatch(ignoreSuggestion(suggestion.id));

    expect(store.getState().suggestions.items).toEqual([suggestion]);
    expect(store.getState().suggestions.error).toBe("Try again");
  });

  it("removes a Gmail item after verification completion succeeds", async () => {
    const gmailSuggestion: SubscriptionSuggestion = {
      ...suggestion,
      id: "1ad756f0-f584-4b85-9e96-32716d7a5d6e",
      provider: "Gmail",
      eventType: "GMAIL_VERIFICATION",
      actionUrl: "https://mail-settings.google.com/mail/vf-test",
    };
    vi.mocked(getPendingSuggestions).mockResolvedValue([gmailSuggestion]);
    vi.mocked(completeGmailVerificationRequest).mockResolvedValue();
    const store = createStore();
    await store.dispatch(fetchSuggestions());

    await store.dispatch(completeGmailVerification(gmailSuggestion.id));

    expect(completeGmailVerificationRequest).toHaveBeenCalledWith(gmailSuggestion.id);
    expect(store.getState().suggestions.items).toEqual([]);
  });
});
