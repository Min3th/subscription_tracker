import { configureStore } from "@reduxjs/toolkit";
import counterReducer from "../features/counter/counterSlice";
import authReducer from "./authSlice";
import preferencesReducer from "../features/preferences/preferencesSlice";
import subscriptionReducer from "./subscriptionSlice";
import suggestionsReducer from "../features/suggestions/suggestionsSlice";

export const store = configureStore({
  reducer: {
    counter: counterReducer,
    auth: authReducer,
    preferences: preferencesReducer,
    subscriptions: subscriptionReducer,
    suggestions: suggestionsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
