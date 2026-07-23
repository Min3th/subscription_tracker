import { expect, test } from "@playwright/test";
import { mockAnonymousApi } from "./fixtures/api";

test.beforeEach(async ({ page }) => {
  await mockAnonymousApi(page);
});

test("public landing page renders and opens sign in", async ({ page }) => {
  await page.goto("/");

  await expect(
    page.getByRole("heading", { name: /track subscriptions effortlessly/i }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Register Now" }).click();
  await expect(page.getByRole("dialog")).toContainText(
    "Sign in to track subscriptions",
  );
  await expect(page.getByRole("button", { name: "Close sign in" })).toBeVisible();
});

test("anonymous visitors cannot access protected routes", async ({ page }) => {
  await page.goto("/dashboard");

  await expect(page).toHaveURL("/");
  await expect(
    page.getByRole("heading", { name: /track subscriptions effortlessly/i }),
  ).toBeVisible();
});

test("landing page supports keyboard access to the registration dialog", async ({
  page,
}) => {
  await page.goto("/");

  const registerButton = page.getByRole("button", { name: "Register Now" });
  await registerButton.focus();
  await page.keyboard.press("Enter");

  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await page.keyboard.press("Tab");
  await expect
    .poll(() => page.evaluate(() => document.activeElement?.closest('[role="dialog"]') !== null))
    .toBe(true);

  await page.keyboard.press("Escape");
  await expect(dialog).toBeHidden();
});
