import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import basicSsl from "@vitejs/plugin-basic-ssl";
import { analyzer } from "vite-bundle-analyzer";

export default defineConfig(({ mode }) => ({
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
        target: "http://localhost:8080",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
}));
