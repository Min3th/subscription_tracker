import { defineConfig, devices, type Project } from "@playwright/test";

const optionalBraveProject: Project[] = process.env.BRAVE_EXECUTABLE_PATH
  ? [
      {
        name: "brave",
        use: {
          ...devices["Desktop Chrome"],
          launchOptions: {
            executablePath: process.env.BRAVE_EXECUTABLE_PATH,
          },
        },
      },
    ]
  : [];

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI
    ? [["line"], ["html", { open: "never" }]]
    : [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: "https://127.0.0.1:4173",
    ignoreHTTPSErrors: true,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  expect: {
    timeout: 10_000,
  },
  webServer: process.env.PLAYWRIGHT_EXTERNAL_SERVER
    ? undefined
    : {
        command: "node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 4173",
        url: "https://127.0.0.1:4173",
        ignoreHTTPSErrors: true,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
        env: {
          ...process.env,
          VITE_API_BASE_URL: "/api",
          VITE_GOOGLE_CLIENT_ID: "playwright-e2e-client-id",
        },
      },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "firefox",
      use: { ...devices["Desktop Firefox"] },
    },
    {
      name: "webkit",
      use: { ...devices["Desktop Safari"] },
    },
    {
      name: "google-chrome",
      use: { ...devices["Desktop Chrome"], channel: "chrome" },
    },
    ...optionalBraveProject,
  ],
});
