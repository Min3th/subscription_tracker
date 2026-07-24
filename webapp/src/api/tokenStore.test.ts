import { afterEach, describe, expect, it } from "vitest";
import { tokenStore } from "./tokenStore";

describe("tokenStore", () => {
  afterEach(() => tokenStore.clear());

  it("holds the access token in memory and clears it", () => {
    expect(tokenStore.get()).toBeNull();
    tokenStore.set("access-token");
    expect(tokenStore.get()).toBe("access-token");
    tokenStore.clear();
    expect(tokenStore.get()).toBeNull();
  });

  it("does not persist access tokens in browser storage", () => {
    tokenStore.set("sensitive-access-token");

    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });
});
