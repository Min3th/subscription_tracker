import { spawn } from "node:child_process";
import https from "node:https";
import process from "node:process";

const host = "127.0.0.1";
const port = 4173;
const baseUrl = `https://${host}:${port}`;
const cwd = process.cwd();

const server = spawn(
  process.execPath,
  ["node_modules/vite/bin/vite.js", "--host", host, "--port", String(port)],
  {
    cwd,
    stdio: "inherit",
    env: {
      ...process.env,
      VITE_API_BASE_URL: "/api",
      VITE_GOOGLE_CLIENT_ID: "playwright-e2e-client-id",
    },
  },
);

const canReachServer = () =>
  new Promise((resolve) => {
    const request = https.get(
      baseUrl,
      { rejectUnauthorized: false, timeout: 1_000 },
      (response) => {
        response.resume();
        resolve(response.statusCode !== undefined && response.statusCode < 500);
      },
    );
    request.on("timeout", () => {
      request.destroy();
      resolve(false);
    });
    request.on("error", () => resolve(false));
  });

const waitForServer = async () => {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    if (await canReachServer()) return;
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`E2E server did not become ready at ${baseUrl}`);
};

let exitCode = 1;

try {
  await waitForServer();

  exitCode = await new Promise((resolve, reject) => {
    const playwright = spawn(
      process.execPath,
      ["node_modules/@playwright/test/cli.js", "test", ...process.argv.slice(2)],
      {
        cwd,
        stdio: "inherit",
        env: {
          ...process.env,
          PLAYWRIGHT_EXTERNAL_SERVER: "1",
        },
      },
    );
    playwright.on("error", reject);
    playwright.on("exit", (code) => resolve(code ?? 1));
  });
} finally {
  server.kill();
}

process.exitCode = exitCode;
