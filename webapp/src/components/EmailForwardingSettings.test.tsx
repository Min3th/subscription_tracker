import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import "../i18n";
import { SnackbarProvider } from "../utils/Snackbar";
import {
  createInboundEmailAddress,
  getInboundEmailAddress,
  revokeInboundEmailAddress,
  rotateInboundEmailAddress,
} from "../api/inboundEmail";
import EmailForwardingSettings from "./EmailForwardingSettings";

vi.mock("../api/inboundEmail", () => ({
  getInboundEmailAddress: vi.fn(),
  createInboundEmailAddress: vi.fn(),
  rotateInboundEmailAddress: vi.fn(),
  revokeInboundEmailAddress: vi.fn(),
}));

const currentAddress = {
  active: true,
  address: "sub-current-token@inbound.subtrak.me",
  createdAt: "2026-07-28T04:00:00Z",
};

const renderSettings = () =>
  render(
    <SnackbarProvider>
      <EmailForwardingSettings />
    </SnackbarProvider>,
  );

describe("EmailForwardingSettings", () => {
  beforeEach(() => {
    vi.mocked(getInboundEmailAddress).mockReset();
    vi.mocked(createInboundEmailAddress).mockReset();
    vi.mocked(rotateInboundEmailAddress).mockReset();
    vi.mocked(revokeInboundEmailAddress).mockReset();
  });

  it("generates an address from the inactive state", async () => {
    const user = userEvent.setup();
    vi.mocked(getInboundEmailAddress).mockResolvedValue({
      active: false,
      address: null,
      createdAt: null,
    });
    vi.mocked(createInboundEmailAddress).mockResolvedValue(currentAddress);

    renderSettings();

    await user.click(
      await screen.findByRole("button", { name: "Generate forwarding address" }),
    );

    expect(createInboundEmailAddress).toHaveBeenCalledOnce();
    expect(
      await screen.findByRole("textbox", { name: "Your private forwarding address" }),
    ).toHaveValue(currentAddress.address);
  });

  it("copies an active address without making another API request", async () => {
    const user = userEvent.setup();
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    vi.mocked(getInboundEmailAddress).mockResolvedValue(currentAddress);

    renderSettings();

    await user.click(await screen.findByRole("button", { name: "Copy address" }));

    expect(writeText).toHaveBeenCalledWith(currentAddress.address);
    expect(createInboundEmailAddress).not.toHaveBeenCalled();
  });

  it("requires confirmation before rotating the address", async () => {
    const user = userEvent.setup();
    const rotatedAddress = {
      ...currentAddress,
      address: "sub-rotated-token@inbound.subtrak.me",
    };
    vi.mocked(getInboundEmailAddress).mockResolvedValue(currentAddress);
    vi.mocked(rotateInboundEmailAddress).mockResolvedValue(rotatedAddress);

    renderSettings();

    await user.click(await screen.findByRole("button", { name: "Rotate address" }));
    expect(rotateInboundEmailAddress).not.toHaveBeenCalled();

    const dialog = screen.getByRole("dialog", { name: "Rotate forwarding address?" });
    await user.click(within(dialog).getByRole("button", { name: "Rotate" }));

    expect(rotateInboundEmailAddress).toHaveBeenCalledOnce();
    expect(
      await screen.findByRole("textbox", { name: "Your private forwarding address" }),
    ).toHaveValue(rotatedAddress.address);
  });

  it("revokes an address only after confirmation", async () => {
    const user = userEvent.setup();
    vi.mocked(getInboundEmailAddress).mockResolvedValue(currentAddress);
    vi.mocked(revokeInboundEmailAddress).mockResolvedValue();

    renderSettings();

    await user.click(await screen.findByRole("button", { name: "Revoke address" }));
    const dialog = screen.getByRole("dialog", { name: "Revoke forwarding address?" });
    await user.click(within(dialog).getByRole("button", { name: "Revoke" }));

    expect(revokeInboundEmailAddress).toHaveBeenCalledOnce();
    expect(
      await screen.findByRole("button", { name: "Generate forwarding address" }),
    ).toBeInTheDocument();
  });

  it("offers a retry when loading fails", async () => {
    const user = userEvent.setup();
    vi.mocked(getInboundEmailAddress)
      .mockRejectedValueOnce(new Error("network"))
      .mockResolvedValueOnce({
        active: false,
        address: null,
        createdAt: null,
      });

    renderSettings();

    await user.click(await screen.findByRole("button", { name: "Retry" }));

    expect(getInboundEmailAddress).toHaveBeenCalledTimes(2);
    expect(
      await screen.findByRole("button", { name: "Generate forwarding address" }),
    ).toBeInTheDocument();
  });
});
