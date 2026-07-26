import { configureStore } from "@reduxjs/toolkit";
import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "../../api/client";
import preferencesReducer, { fetchPreferences, updatePreferences } from "./preferencesSlice";

vi.mock("../../api/client", () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

const serverPreferences = {
  currency: "USD",
  language: "en",
  timezone: "UTC",
  theme: "light",
  emailNotificationsEnabled: true,
  reminderDaysBefore: 3,
  reminderTime: "09:00:00",
  onboardingCompleted: false,
};

describe("preferences request coordination", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("deduplicates preference fetches while the first request is active", async () => {
    let resolveRequest!: (value: { data: typeof serverPreferences }) => void;
    vi.mocked(api.get).mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve;
      }),
    );
    const store = configureStore({ reducer: { preferences: preferencesReducer } });

    const first = store.dispatch(fetchPreferences());
    const duplicate = store.dispatch(fetchPreferences());
    resolveRequest({ data: serverPreferences });
    await Promise.all([first, duplicate]);

    expect(api.get).toHaveBeenCalledTimes(1);
    expect(store.getState().preferences.onboardingCompleted).toBe(false);
    expect(store.getState().preferences.status).toBe("succeeded");
  });

  it("does not overwrite loading preferences with a default-based update", async () => {
    let resolveRequest!: (value: { data: typeof serverPreferences }) => void;
    vi.mocked(api.get).mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve;
      }),
    );
    const store = configureStore({ reducer: { preferences: preferencesReducer } });

    const fetch = store.dispatch(fetchPreferences());
    const overlappingUpdate = store.dispatch(updatePreferences({ theme: "dark" }));
    resolveRequest({ data: serverPreferences });
    await Promise.all([fetch, overlappingUpdate]);

    expect(api.put).not.toHaveBeenCalled();
    expect(store.getState().preferences.onboardingCompleted).toBe(false);
  });
});
