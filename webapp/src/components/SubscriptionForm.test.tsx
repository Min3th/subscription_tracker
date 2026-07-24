import { configureStore } from "@reduxjs/toolkit";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { axe } from "jest-axe";
import { Provider } from "react-redux";
import { describe, expect, it, vi } from "vitest";
import authReducer from "../app/authSlice";
import subscriptionReducer from "../app/subscriptionSlice";
import counterReducer from "../features/counter/counterSlice";
import preferencesReducer from "../features/preferences/preferencesSlice";
import { SnackbarProvider } from "../utils/Snackbar";
import SubscriptionForm from "./SubscriptionForm";

const renderForm = () => {
  const store = configureStore({
    reducer: {
      auth: authReducer,
      counter: counterReducer,
      preferences: preferencesReducer,
      subscriptions: subscriptionReducer,
    },
  });
  const handleClose = vi.fn();

  const result = render(
    <Provider store={store}>
      <SnackbarProvider>
        <SubscriptionForm open handleClose={handleClose} />
      </SnackbarProvider>
    </Provider>,
  );

  return { ...result, handleClose };
};

describe("SubscriptionForm", () => {
  it("exposes the dialog and fundamental controls by accessible names", async () => {
    const { container } = renderForm();

    expect(screen.getByRole("dialog", { name: "Add Subscription" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Subscription Name" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Category" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Type" })).toBeInTheDocument();
    expect(screen.getByRole("spinbutton", { name: "Billing interval count" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Billing interval unit" })).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });

  it("blocks access to billing details until required basic fields are valid", async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByText("Name is required")).toBeInTheDocument();
    expect(screen.getByText("Category is required")).toBeInTheDocument();
    expect(screen.queryByRole("spinbutton", { name: /^Amount/ })).not.toBeInTheDocument();
  });

  it("supports keyboard dismissal through the Cancel button", async () => {
    const user = userEvent.setup();
    const { handleClose } = renderForm();

    await user.tab();
    const cancel = screen.getByRole("button", { name: "Cancel" });
    cancel.focus();
    await user.keyboard("{Enter}");

    expect(handleClose).toHaveBeenCalledOnce();
  });
});
