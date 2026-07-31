import { configureStore } from "@reduxjs/toolkit";
import { Provider } from "react-redux";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { axe } from "jest-axe";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  confirmSuggestion,
  completeGmailVerification,
  getPendingSuggestions,
  ignoreSuggestion,
} from "../api/inboundEmail";
import suggestionsReducer from "../features/suggestions/suggestionsSlice";
import type { SubscriptionSuggestion } from "../types/suggestion";
import { SnackbarProvider } from "../utils/Snackbar";
import Suggestions from "./Suggestions";

vi.mock("../api/inboundEmail", () => ({
  getPendingSuggestions: vi.fn(),
  confirmSuggestion: vi.fn(),
  completeGmailVerification: vi.fn(),
  ignoreSuggestion: vi.fn(),
}));

const suggestion: SubscriptionSuggestion = {
  id: "8852473b-2e55-4fa6-b1be-4b999670f4ed",
  provider: "Acme",
  planName: "Pro",
  amount: 19.99,
  currency: "USD",
  billingIntervalUnit: "month",
  billingIntervalCount: 1,
  startDate: "2026-07-28",
  renewalDate: "2026-08-28",
  eventType: "NEW_SUBSCRIPTION",
  confidence: 0.91,
  evidenceSummary: "Acme Pro renews monthly.",
  actionUrl: null,
  status: "PENDING",
  possibleDuplicate: null,
  receivedAt: "2026-07-28T08:00:00Z",
  createdAt: "2026-07-28T08:01:00Z",
};

const renderPage = () => {
  const store = configureStore({
    reducer: { suggestions: suggestionsReducer },
  });
  const view = render(
    <Provider store={store}>
      <SnackbarProvider>
        <Suggestions />
      </SnackbarProvider>
    </Provider>,
  );
  return { store, ...view };
};

describe("Suggestions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getPendingSuggestions).mockResolvedValue([suggestion]);
  });

  it("reviews and confirms edited extracted values", async () => {
    vi.mocked(confirmSuggestion).mockResolvedValue({ id: 8 } as never);
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Acme — Pro")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Review and confirm" }));
    const name = screen.getByRole("textbox", { name: "Subscription name" });
    await user.clear(name);
    await user.type(name, "Acme Team");
    await user.click(screen.getByRole("button", { name: "Confirm and add" }));

    await waitFor(() =>
      expect(confirmSuggestion).toHaveBeenCalledWith(
        suggestion.id,
        expect.objectContaining({ name: "Acme Team", cost: 19.99 }),
      ),
    );
    expect(await screen.findByText("You're all caught up")).toBeInTheDocument();
  });

  it("requires confirmation before ignoring a suggestion", async () => {
    vi.mocked(ignoreSuggestion).mockResolvedValue();
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Acme — Pro");
    await user.click(screen.getByRole("button", { name: "Ignore" }));
    expect(ignoreSuggestion).not.toHaveBeenCalled();
    await user.click(screen.getByRole("button", { name: "Ignore" }));

    await waitFor(() => expect(ignoreSuggestion).toHaveBeenCalledWith(suggestion.id));
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderPage();
    await screen.findByText("Acme — Pro");

    expect(await axe(container)).toHaveNoViolations();
  });

  it("shows Gmail verification without exposing subscription confirmation", async () => {
    const gmailSuggestion: SubscriptionSuggestion = {
      ...suggestion,
      id: "2b54e69d-e1c5-43d6-b8e8-5f450c400ca9",
      provider: "Gmail",
      planName: null,
      amount: null,
      currency: null,
      eventType: "GMAIL_VERIFICATION",
      actionUrl: "https://mail-settings.google.com/mail/vf-safe-token",
    };
    vi.mocked(getPendingSuggestions).mockResolvedValue([gmailSuggestion]);
    vi.mocked(completeGmailVerification).mockResolvedValue();
    const user = userEvent.setup();
    renderPage();

    const link = await screen.findByRole("link", { name: "Open Google verification" });
    expect(link).toHaveAttribute(
      "href",
      "https://mail-settings.google.com/mail/vf-safe-token",
    );
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
    expect(screen.queryByRole("button", { name: "Review and confirm" })).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "I've completed verification" }));
    await waitFor(() =>
      expect(completeGmailVerification).toHaveBeenCalledWith(gmailSuggestion.id),
    );
  });

  it("does not render an untrusted Gmail action URL", async () => {
    vi.mocked(getPendingSuggestions).mockResolvedValue([{
      ...suggestion,
      provider: "Gmail",
      planName: null,
      eventType: "GMAIL_VERIFICATION",
      actionUrl: "https://mail-settings.google.com.evil.example/mail/vf-token",
    }]);
    renderPage();

    expect(await screen.findByText(/did not contain a trusted Google verification link/i))
      .toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Open Google verification" }))
      .not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "I've completed verification" }))
      .toBeDisabled();
  });
});
