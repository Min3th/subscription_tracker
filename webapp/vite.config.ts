import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import basicSsl from "@vitejs/plugin-basic-ssl";
import { analyzer } from "vite-bundle-analyzer";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "VITE_");
  const apiBaseUrl = env.VITE_API_BASE_URL?.trim() || "/api";
  const apiProxyTarget =
    env.VITE_API_PROXY_TARGET?.trim() || "http://localhost:8080";
  const parsedProxyTarget = new URL(apiProxyTarget);

  if (!["http:", "https:"].includes(parsedProxyTarget.protocol)) {
    throw new Error("VITE_API_PROXY_TARGET must use http or https");
  }

  if (!apiBaseUrl.startsWith("/")) {
    const parsedApiBaseUrl = new URL(apiBaseUrl);

    if (!["http:", "https:"].includes(parsedApiBaseUrl.protocol)) {
      throw new Error("VITE_API_BASE_URL must use http or https");
    }
    if (parsedApiBaseUrl.hostname === "subtrak-api.duckdns.org") {
      throw new Error(
        "VITE_API_BASE_URL still uses the retired DuckDNS backend. Update the deployment environment before building.",
      );
    }
  }

  // This URL is public configuration compiled into the browser bundle. Logging
  // it makes deployment-environment conflicts diagnosable without exposing a
  // credential or another sensitive value.
  console.info(`[vite] API base URL: ${apiBaseUrl}`);

  return {
    plugins: [
      react(),
      basicSsl(),
      analyzer({
        enabled: mode === "analyze",
        analyzerMode: "static",
        fileName: "bundle-report",
        openAnalyzer: false,
        defaultSizes: "gzip",
        reportTitle: "SubTrak bundle analysis",
      }),
    ],
    test: {
      include: ["src/**/*.test.{ts,tsx}"],
      environment: "jsdom",
      setupFiles: ["./src/test/setup.ts"],
      restoreMocks: true,
      clearMocks: true,
      css: true,
    },
    server: {
      https: {},
      proxy: {
        "/api": {
          target: parsedProxyTarget.origin,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ""),
        },
      },
    },
  };
});
