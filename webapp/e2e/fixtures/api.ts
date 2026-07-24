import type { Page, Route } from "@playwright/test";

const user = {
  id: 1,
  name: "Ada Tester",
  email: "ada@example.com",
  picture: "",
};

const preferences = {
  currency: "USD",
  language: "en",
  timezone: "Asia/Colombo",
  theme: "light",
  emailNotificationsEnabled: true,
  reminderDaysBefore: 3,
};

const subscriptions = [
  {
    id: 101,
    name: "Very Long Subscription Service Name",
    cost: "19.9900",
    currency: "USD",
    type: "recurring",
    category: "Entertainment",
    description: "Family streaming plan with a deliberately long mobile description.",
    paymentMethod: "Personal card",
    website: "https://example.com/a/very/long/subscription/path",
    startDate: "2026-01-01",
    billingIntervalUnit: "month",
    billingIntervalCount: 1,
    nextBillingDate: "2026-08-01",
    totalPaid: "139.9300",
    emailNotificationsEnabled: true,
  },
];

const fulfillJson = (route: Route, body: unknown, status = 200) =>
  route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });

export async function mockAuthenticatedApi(page: Page) {
  await page.route(/\/api\/(?:auth|user|subscriptions)(?:\/|$)/, async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace(/^\/api/, "");
    const method = request.method();

    if (path === "/auth/refresh" && method === "POST") {
      return fulfillJson(route, { accessToken: "e2e-access-token", user });
    }

    if (path === "/user/preferences" && method === "GET") {
      return fulfillJson(route, preferences);
    }

    if (path === "/subscriptions" && method === "GET") {
      return fulfillJson(route, subscriptions);
    }

    if (path === "/subscriptions" && method === "POST") {
      return fulfillJson(
        route,
        {
          ...request.postDataJSON(),
          id: 102,
          nextBillingDate: "2026-09-01",
          totalPaid: "0.0000",
        },
        201,
      );
    }

    return fulfillJson(route, {});
  });
}

export async function mockAnonymousApi(page: Page) {
  await page.route(/\/api\/auth\/refresh$/, (route) =>
    fulfillJson(route, { message: "No refresh session" }, 401),
  );
}
