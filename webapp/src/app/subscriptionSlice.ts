import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { deleteSubscription, getSubscriptions } from "../api/subscription";
import { getNextBillingDate, calculateTotalPaid } from "../pages/Subscriptions";
import type { DetailedSubscription } from "../types/subscription";

export const fetchSubscriptions = createAsyncThunk("subscriptions/fetch", async () => {
  const res = await getSubscriptions();
  return res.data.map((item: any) => ({
    id: item.id, // keep as number (IMPORTANT)
    name: item.name,
    cost: item.cost,
    billingIntervalUnit: item.billingIntervalUnit,
    billingIntervalCount: item.billingIntervalCount,
    nextBillingDate: getNextBillingDate(item.startDate, item.billingIntervalUnit, item.billingIntervalCount),
    category: item.category || "General",
    status: "active",
    paymentMethod: item.paymentMethod,
    startDate: item.startDate,
    description: item.description,
    website: item.website,
    autoRenew: item.type === "recurring",
    totalPaid: calculateTotalPaid(item.startDate, item.billingIntervalUnit, item.billingIntervalCount, item.cost),
  }));
});

export const deleteSubscriptionThunk = createAsyncThunk("subscriptions/delete", async (id: number) => {
  await deleteSubscription(id);
  return id;
});

interface State {
  list: DetailedSubscription[];
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
