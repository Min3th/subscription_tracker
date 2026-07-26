import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { axe } from "jest-axe";
import { describe, expect, it, vi } from "vitest";
import "../i18n";
import WelcomeGuide from "./WelcomeGuide";

describe("WelcomeGuide", () => {
  it("moves through the four concise onboarding slides", async () => {
    const user = userEvent.setup();
    render(<WelcomeGuide open onComplete={vi.fn()} />);

    expect(screen.getByRole("heading", { name: "Welcome to Subtrak" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Next" }));
    expect(screen.getByRole("heading", { name: "Add what you pay for" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Next" }));
    expect(screen.getByRole("heading", { name: "Understand your spending" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Next" }));
    expect(screen.getByRole("heading", { name: "Stay ahead of renewals" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add your first subscription" })).toBeInTheDocument();
  });

  it("allows users to skip immediately", async () => {
    const user = userEvent.setup();
    const onComplete = vi.fn();
    render(<WelcomeGuide open onComplete={onComplete} />);

    await user.click(screen.getByRole("button", { name: "Skip guide" }));

    expect(onComplete).toHaveBeenCalledWith("dismiss");
  });

  it("finishes into the add-subscription action", async () => {
    const user = userEvent.setup();
    const onComplete = vi.fn();
    render(<WelcomeGuide open onComplete={onComplete} />);

    for (let step = 0; step < 3; step += 1) {
      await user.click(screen.getByRole("button", { name: "Next" }));
    }
    await user.click(screen.getByRole("button", { name: "Add your first subscription" }));

    expect(onComplete).toHaveBeenCalledWith("add-subscription");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<WelcomeGuide open onComplete={vi.fn()} />);

    expect(await axe(container)).toHaveNoViolations();
  });
});
