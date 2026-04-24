import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { deleteSubscription, getSubscriptions } from "../api/subscription";

export const fetchSubscriptions = createAsyncThunk("subscriptions/fetch", async () => {
  const res = await getSubscriptions();
  return res.data;
});

export const deleteSubscriptionThunk = createAsyncThunk("subscriptions/delete", async (id: number) => {
  await deleteSubscription(id);
  return id;
});

interface Subscription {
  id: number;
  name: string;
}

interface State {
  list: Subscription[];
  status: "idle" | "loading";
}

const initialState: State = {
  list: [],
  status: "idle",
};

const slice = createSlice({
  name: "subscriptions",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchSubscriptions.fulfilled, (state, action) => {
        state.list = action.payload;
      })
      .addCase(deleteSubscriptionThunk.fulfilled, (state, action) => {
        state.list = state.list.filter((sub) => sub.id !== action.payload);
      });
  },
});

export default slice.reducer;
