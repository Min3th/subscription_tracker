import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";
import { mockAnonymousApi, mockAuthenticatedApi } from "./fixtures/api";

test("public page has no automatically detectable serious accessibility violations", async ({
  page,
}) => {
  await mockAnonymousApi(page);
  await page.goto("/");
  await expect(page.getByRole("main")).toBeVisible();

  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();

  expect(
    results.violations.filter(({ impact }) => impact === "serious" || impact === "critical"),
  ).toEqual([]);
});

test("subscription page has no automatically detectable serious accessibility violations", async ({
  page,
}) => {
  await mockAuthenticatedApi(page);
  await page.goto("/subscriptions");
  await expect(page.getByText("Netflix", { exact: true })).toBeVisible();

  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();

  expect(
    results.violations.filter(({ impact }) => impact === "serious" || impact === "critical"),
  ).toEqual([]);
});
