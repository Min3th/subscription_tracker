import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../api/client";
import { tokenStore } from "../api/tokenStore";

type User = {
  name: string;
  email: string;
  picture: string;
};

type AuthState = {
  user: User | null;
  token: string | null;
  sessionStatus: "idle" | "loading" | "ready";
};

const initialState: AuthState = {
  user: null,
  token: null,
  sessionStatus: "idle",
};

export const initializeSession = createAsyncThunk(
  "auth/initializeSession",
  async () => {
    const response = await api.post("/auth/refresh");
    tokenStore.set(response.data.accessToken);
    return response.data as { accessToken: string; user: User };
  },
  {
    condition: (_, { getState }) => {
      const state = getState() as { auth: AuthState };
      return state.auth.sessionStatus === "idle";
    },
  },
);

export const logoutUser = createAsyncThunk("auth/logoutUser", async () => {
  try {
    await api.post("/auth/logout");
  } catch (e) {
    console.warn("Logout request failed");
  }

  tokenStore.clear();

  return true;
});

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setAuth: (state, action) => {
      state.user = action.payload.user;
      state.token = action.payload.token;
      state.sessionStatus = "ready";
      tokenStore.set(action.payload.token);
    },
    logout: (state) => {
      state.user = null;
      state.token = null;
      state.sessionStatus = "ready";
      tokenStore.clear();
    },
  },
  extraReducers: (builder) => {
    builder.addCase(logoutUser.fulfilled, (state) => {
      state.user = null;
      state.token = null;
      state.sessionStatus = "ready";
    });
    builder.addCase(initializeSession.pending, (state) => {
      state.sessionStatus = "loading";
    });
    builder.addCase(initializeSession.fulfilled, (state, action) => {
      state.user = action.payload.user;
      state.token = action.payload.accessToken;
      state.sessionStatus = "ready";
    });
    builder.addCase(initializeSession.rejected, (state) => {
      state.user = null;
      state.token = null;
      state.sessionStatus = "ready";
      tokenStore.clear();
    });
  },
});

export const { setAuth, logout } = authSlice.actions;
export default authSlice.reducer;
