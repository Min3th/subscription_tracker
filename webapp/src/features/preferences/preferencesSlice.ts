import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import type { PayloadAction } from "@reduxjs/toolkit";
import api from "../../api/client";

export interface PreferencesState {
  currency: string;
  language: string;
  timezone: string;
  theme: string;
  status: "idle" | "loading" | "succeeded" | "failed";
  error: string | null;
}

const initialState: PreferencesState = {
  currency: "USD",
  language: "en",
  timezone: "America/New_York",
  theme: "auto",
  status: "idle",
  error: null,
};

export const fetchPreferences = createAsyncThunk("preferences/fetchPreferences", async () => {
  const response = await api.get("/user/preferences");
  return response.data;
});

export const updatePreferences = createAsyncThunk(
  "preferences/updatePreferences",
  async (data: Partial<PreferencesState>) => {
    const response = await api.put("/user/preferences", data);
    return response.data;
  },
);

const preferencesSlice = createSlice({
  name: "preferences",
  initialState,
  reducers: {
    setPreferences(state, action: PayloadAction<Partial<PreferencesState>>) {
      if (action.payload.currency) state.currency = action.payload.currency;
      if (action.payload.language) state.language = action.payload.language;
      if (action.payload.timezone) state.timezone = action.payload.timezone;
      if (action.payload.theme) state.theme = action.payload.theme;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchPreferences.pending, (state) => {
        state.status = "loading";
      })
      .addCase(fetchPreferences.fulfilled, (state, action) => {
        state.status = "succeeded";
        state.currency = action.payload.currency || state.currency;
        state.language = action.payload.language || state.language;
        state.timezone = action.payload.timezone || state.timezone;
        state.theme = action.payload.theme || state.theme;
      })
      .addCase(fetchPreferences.rejected, (state, action) => {
        state.status = "failed";
        state.error = action.error.message || "Failed to fetch preferences";
      })
      .addCase(updatePreferences.pending, (state) => {
        state.status = "loading";
      })
      .addCase(updatePreferences.fulfilled, (state, action) => {
        state.status = "succeeded";
        state.currency = action.payload.currency || state.currency;
        state.language = action.payload.language || state.language;
        state.timezone = action.payload.timezone || state.timezone;
        state.theme = action.payload.theme || state.theme;
      })
      .addCase(updatePreferences.rejected, (state, action) => {
        state.status = "failed";
        state.error = action.error.message || "Failed to update preferences";
      });
  },
});

export const { setPreferences } = preferencesSlice.actions;
export default preferencesSlice.reducer;
