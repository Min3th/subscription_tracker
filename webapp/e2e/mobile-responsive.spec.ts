import { expect, test } from "@playwright/test";
import { mockAuthenticatedApi } from "./fixtures/api";

test.beforeEach(async ({ page }) => {
  await mockAuthenticatedApi(page);
});

test("core pages do not overflow horizontally", async ({ page }) => {
  for (const path of ["/", "/dashboard", "/subscriptions", "/settings"]) {
    await page.goto(path);
    await expect(page.locator("body")).toBeVisible();

    const dimensions = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));

    expect(dimensions.content, `${path} should fit the viewport`).toBeLessThanOrEqual(
      dimensions.viewport,
    );
  }
});

test("landing hero and primary action stay inside the viewport", async ({ page }) => {
  await page.goto("/");

  const heading = page.getByRole("heading", {
    name: /track subscriptions effortlessly/i,
  });
  const register = page.getByRole("button", { name: "Register Now" });

  await expect(heading).toBeVisible();
  await expect(register).toBeVisible();

  for (const locator of [heading, register]) {
    const box = await locator.boundingBox();
    expect(box).not.toBeNull();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(
      await page.evaluate(() => document.documentElement.clientWidth),
    );
  }

  expect((await register.boundingBox())!.height).toBeGreaterThanOrEqual(44);
});

test("mobile dashboard navigation opens, navigates, and closes", async ({
  page,
}, testInfo) => {
  test.skip(!testInfo.project.name.startsWith("mobile-"), "Mobile navigation test");

  await page.goto("/dashboard");
  await page.getByRole("button", { name: "Open navigation" }).click();

  const closeButton = page.getByRole("button", { name: "Close navigation" });
  await expect(closeButton).toBeVisible();

  await page.getByRole("link", { name: "Subscriptions" }).click();
  await expect(page).toHaveURL(/\/subscriptions$/);
  await expect(closeButton).toBeHidden();
});

test("mobile user can open and complete the add subscription form", async ({
  page,
}, testInfo) => {
  test.skip(!testInfo.project.name.startsWith("mobile-"), "Mobile form test");

  await page.goto("/subscriptions");
  const addButton = page.getByRole("button", { name: "Add Subscription" });
  await expect(addButton).toBeVisible();
  expect((await addButton.boundingBox())!.height).toBeGreaterThanOrEqual(44);

  await addButton.click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();

  await page.getByRole("textbox", { name: "Subscription Name" }).fill("Music Plus");
  await page.getByRole("combobox", { name: "Category" }).click();
  await page.getByRole("option", { name: "Music" }).click();

  const count = page.getByRole("spinbutton", { name: "Billing interval count" });
  const unit = page.getByRole("combobox", { name: "Billing interval unit" });
  expect((await count.boundingBox())!.width).toBeGreaterThanOrEqual(200);
  expect((await unit.boundingBox())!.width).toBeGreaterThanOrEqual(200);

  await page.getByRole("button", { name: "Next" }).click();
  await page.getByRole("spinbutton", { name: "Amount (USD)" }).fill("12.99");
  await page.getByLabel("Start Date").fill("2026-07-23");
  await page.getByRole("button", { name: "Add", exact: true }).click();

  await expect(dialog).toBeHidden();
});
