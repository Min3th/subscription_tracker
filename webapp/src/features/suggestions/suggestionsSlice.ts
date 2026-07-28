import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import {
  confirmSuggestion as confirmSuggestionRequest,
  getPendingSuggestions,
  ignoreSuggestion as ignoreSuggestionRequest,
} from "../../api/inboundEmail";
import type {
  ConfirmSuggestionRequest,
  SubscriptionSuggestion,
} from "../../types/suggestion";

export interface SuggestionsState {
  items: SubscriptionSuggestion[];
  status: "idle" | "loading" | "succeeded" | "failed";
  decidingId: string | null;
  error: string | null;
}

const initialState: SuggestionsState = {
  items: [],
  status: "idle",
  decidingId: null,
  error: null,
};

export const fetchSuggestions = createAsyncThunk(
  "suggestions/fetch",
  getPendingSuggestions,
  {
    condition: (_, { getState }) => {
      const state = getState() as { suggestions: SuggestionsState };
      return state.suggestions.status !== "loading";
    },
  },
);

export const confirmSuggestion = createAsyncThunk(
  "suggestions/confirm",
  async ({ id, request }: { id: string; request: ConfirmSuggestionRequest }) => {
    const subscription = await confirmSuggestionRequest(id, request);
    return { id, subscription };
  },
);

export const ignoreSuggestion = createAsyncThunk(
  "suggestions/ignore",
  async (id: string) => {
    await ignoreSuggestionRequest(id);
    return id;
  },
);

const suggestionsSlice = createSlice({
  name: "suggestions",
  initialState,
  reducers: {
    clearSuggestionError(state) {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchSuggestions.pending, (state) => {
        state.status = "loading";
        state.error = null;
      })
      .addCase(fetchSuggestions.fulfilled, (state, action) => {
        state.status = "succeeded";
        state.items = action.payload;
      })
      .addCase(fetchSuggestions.rejected, (state, action) => {
        if (action.meta.condition) return;
        state.status = "failed";
        state.error = action.error.message ?? "Failed to load suggestions";
      })
      .addCase(confirmSuggestion.pending, (state, action) => {
        state.decidingId = action.meta.arg.id;
        state.error = null;
      })
      .addCase(confirmSuggestion.fulfilled, (state, action) => {
        state.decidingId = null;
        state.items = state.items.filter((item) => item.id !== action.payload.id);
      })
      .addCase(confirmSuggestion.rejected, (state, action) => {
        state.decidingId = null;
        state.error = action.error.message ?? "Failed to confirm suggestion";
      })
      .addCase(ignoreSuggestion.pending, (state, action) => {
        state.decidingId = action.meta.arg;
        state.error = null;
      })
      .addCase(ignoreSuggestion.fulfilled, (state, action) => {
        state.decidingId = null;
        state.items = state.items.filter((item) => item.id !== action.payload);
      })
      .addCase(ignoreSuggestion.rejected, (state, action) => {
        state.decidingId = null;
        state.error = action.error.message ?? "Failed to ignore suggestion";
      });
  },
});

export const { clearSuggestionError } = suggestionsSlice.actions;
export default suggestionsSlice.reducer;
