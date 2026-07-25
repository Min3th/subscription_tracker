import { expect, test } from "@playwright/test";
import { mockAuthenticatedApi } from "./fixtures/api";

test.beforeEach(async ({ page }) => {
  await mockAuthenticatedApi(page);
});

test("authenticated user can access and search subscriptions", async ({ page }) => {
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  await page.goto("/subscriptions");
  await page.waitForTimeout(500);

  expect(pageErrors).toEqual([]);
  await expect(page.getByText("Netflix", { exact: true })).toBeVisible();
  await expect(page.getByText("Cloud Storage", { exact: true })).toBeVisible();

  await page.getByPlaceholder("Search subscriptions...").fill("Netflix");

  await expect(page.getByText("Netflix", { exact: true })).toBeVisible();
  await expect(page.getByText("Cloud Storage", { exact: true })).toBeHidden();
});

test("authenticated user can add a recurring subscription", async ({ page }) => {
  await page.goto("/subscriptions");
  await page.getByRole("button", { name: "Add Subscription" }).click();

  await page.getByRole("textbox", { name: "Subscription Name" }).fill("Music Plus");
  await page.getByRole("combobox", { name: "Category" }).click();
  await page.getByRole("option", { name: "Music" }).click();
  await page.getByRole("button", { name: "Next" }).click();

  await page.getByRole("spinbutton", { name: "Amount (USD)" }).fill("12.99");
  await page.getByLabel("Start Date").fill("2026-07-23");
  await page.getByRole("button", { name: "Add", exact: true }).click();

  await expect(page.getByText("Subscription created successfully!")).toBeVisible();
  await expect(page.getByRole("dialog")).toBeHidden();
});

test("authenticated user can change reminder settings", async ({ page }) => {
  await page.goto("/settings");

  await expect(page.getByRole("radio", { name: "3 days before" })).toBeChecked();
  await page.getByRole("radio", { name: "1 day before" }).check();
  await page.getByRole("button", { name: /save/i }).click();

  const confirmation = page.getByRole("dialog");
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole("button", { name: /confirm/i }).click();
  await expect(page.getByText(/saved successfully/i)).toBeVisible();
});
