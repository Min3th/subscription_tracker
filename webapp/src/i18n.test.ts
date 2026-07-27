import { afterEach, describe, expect, it } from "vitest";
import i18n from "./i18n";

describe("translation loading", () => {
  afterEach(async () => {
    await i18n.changeLanguage("en");
  });

  it("loads a locale on demand when the language changes", async () => {
    await i18n.changeLanguage("es");

    expect(i18n.t("dashboard.subtitle")).toBe(
      "Gestione y supervise todas sus suscripciones de manera eficiente",
    );
  });

  it("normalizes regional language codes to a supported locale", async () => {
    await i18n.changeLanguage("de-DE");

    expect(i18n.resolvedLanguage).toBe("de");
  });
});
