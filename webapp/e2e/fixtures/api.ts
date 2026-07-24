import type { Page, Route } from "@playwright/test";

export const testUser = {
  id: 1,
  googleId: "google-test-user",
  name: "Ada Tester",
  email: "ada@example.com",
  picture: "",
};

export const testPreferences = {
  currency: "USD",
  language: "en",
  timezone: "Asia/Colombo",
  theme: "light",
  emailNotificationsEnabled: true,
  reminderDaysBefore: 3,
};

export const testSubscriptions = [
  {
    id: 101,
    name: "Netflix",
    cost: "19.9900",
    currency: "USD",
    type: "recurring",
    category: "Entertainment",
    description: "Family streaming plan",
    paymentMethod: "Personal card",
    website: "https://www.netflix.com",
    startDate: "2026-01-01",
    billingIntervalUnit: "month",
    billingIntervalCount: 1,
    nextBillingDate: "2026-08-01",
    totalPaid: "139.9300",
    emailNotificationsEnabled: true,
  },
  {
    id: 102,
    name: "Cloud Storage",
    cost: "9.5000",
    currency: "USD",
    type: "recurring",
    category: "Productivity",
    description: "Encrypted backups",
    paymentMethod: "Work card",
    website: "https://example.com/storage",
    startDate: "2026-02-15",
    billingIntervalUnit: "month",
    billingIntervalCount: 1,
    nextBillingDate: "2026-08-15",
    totalPaid: "57.0000",
    emailNotificationsEnabled: false,
  },
];

const json = (route: Route, body: unknown, status = 200) =>
  route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });

export async function mockAnonymousApi(page: Page) {
  await page.route("**/api/auth/refresh", (route) =>
    json(route, { message: "No refresh session" }, 401),
  );
}

export async function mockAuthenticatedApi(page: Page) {
  let subscriptions = structuredClone(testSubscriptions);
  let preferences = { ...testPreferences };

  await page.route(/\/api\/(?:auth|user|subscriptions)(?:\/|$)/, async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace(/^\/api/, "");
    const method = request.method();

    if (path === "/auth/refresh" && method === "POST") {
      return json(route, { accessToken: "e2e-access-token", user: testUser });
    }

    if (path === "/user/preferences" && method === "GET") {
      return json(route, preferences);
    }

    if (path === "/user/preferences" && method === "PUT") {
      preferences = { ...preferences, ...request.postDataJSON() };
      return json(route, preferences);
    }

    if (path === "/subscriptions" && method === "GET") {
      return json(route, subscriptions);
    }

    if (path === "/subscriptions" && method === "POST") {
      const created = {
        ...request.postDataJSON(),
        id: 103,
        nextBillingDate: "2026-09-01",
        totalPaid: "0.0000",
      };
      subscriptions.push(created);
      return json(route, created, 201);
    }

    const subscriptionMatch = path.match(/^\/subscriptions\/(\d+)$/);
    if (subscriptionMatch) {
      const id = Number(subscriptionMatch[1]);
      const existing = subscriptions.find((subscription) => subscription.id === id);

      if (method === "GET") {
        return existing
          ? json(route, existing)
          : json(route, { message: "Not found" }, 404);
      }

      if (method === "PUT" && existing) {
        const updated = { ...existing, ...request.postDataJSON(), id };
        subscriptions = subscriptions.map((subscription) =>
          subscription.id === id ? updated : subscription,
        );
        return json(route, updated);
      }

      if (method === "DELETE") {
        subscriptions = subscriptions.filter((subscription) => subscription.id !== id);
        return route.fulfill({ status: 204 });
      }
    }

    return json(route, { message: `Unhandled E2E route: ${method} ${path}` }, 501);
  });
}
