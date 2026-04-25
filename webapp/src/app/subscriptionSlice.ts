import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { deleteSubscription, getSubscriptions, updateSubscriptions } from "../api/subscription";
import type { DetailedSubscription, UpdateSubscriptionPayload } from "../types/subscription";

const mapToDetailed = (item: any): DetailedSubscription => ({
  id: item.id,
  name: item.name,
  cost: item.cost,
  billingIntervalUnit: item.billingIntervalUnit,
  billingIntervalCount: item.billingIntervalCount,
  nextBillingDate: new Date(item.nextBillingDate),
  category: item.category || "General",
  status: "active",
  type: item.type,
  paymentMethod: item.paymentMethod,
  startDate: item.startDate,
  description: item.description,
  website: item.website,
  autoRenew: item.type === "recurring",
  totalPaid: item.totalPaid,
});

export const fetchSubscriptions = createAsyncThunk("subscriptions/fetch", async () => {
  const res = await getSubscriptions();
  console.log("Res: ", res);
  return res.data.map(mapToDetailed);
});

export const deleteSubscriptionThunk = createAsyncThunk("subscriptions/delete", async (id: number) => {
  await deleteSubscription(id);
  return id;
});

export const updateSubscriptionThunk = createAsyncThunk(
  "subscriptions/update",
  async (subscriptionData: UpdateSubscriptionPayload) => {
    const response = await updateSubscriptions(subscriptionData);
    return response.data;
  },
);

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
      })
      .addCase(updateSubscriptionThunk.fulfilled, (state, action) => {
        const index = state.list.findIndex((sub) => sub.id === action.payload.id);
        if (index !== -1) {
          state.list[index] = mapToDetailed(action.payload);
        }
      });
  },
});

export default slice.reducer;
