import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import basicSsl from "@vitejs/plugin-basic-ssl";
import { analyzer } from "vite-bundle-analyzer";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "VITE_");
  const apiProxyTarget =
    env.VITE_API_PROXY_TARGET?.trim() || "http://localhost:8080";
  const parsedProxyTarget = new URL(apiProxyTarget);

  if (!["http:", "https:"].includes(parsedProxyTarget.protocol)) {
    throw new Error("VITE_API_PROXY_TARGET must use http or https");
  }

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
