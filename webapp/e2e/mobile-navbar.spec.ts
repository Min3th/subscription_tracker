import { expect, test } from "@playwright/test";
import { mockAnonymousApi } from "./fixtures/api";

test.beforeEach(async ({ page }, testInfo) => {
  test.skip(!testInfo.project.name.startsWith("mobile-"), "Mobile navbar test");
  await mockAnonymousApi(page);
});

test("anonymous mobile navbar exposes sections and login", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Open navigation menu" }).click();

  const menu = page.getByRole("menu");
  await expect(menu.getByRole("menuitem", { name: "Why Track" })).toBeVisible();
  await expect(menu.getByRole("menuitem", { name: "Features" })).toBeVisible();
  await expect(menu.getByRole("menuitem", { name: "How It Works" })).toBeVisible();

  await menu.getByRole("menuitem", { name: "Login" }).click();
  await expect(page.getByRole("dialog")).toContainText("Sign in to track subscriptions");
});
