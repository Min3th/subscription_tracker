import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../api/client";

type User = {
  name: string;
  email: string;
  picture: string;
};

type AuthState = {
  user: User | null;
  token: string | null;
};

const initialState: AuthState = {
  user: null,
  token: null,
};

export const logoutUser = createAsyncThunk("auth/logoutUser", async (_, { dispatch }) => {
  try {
    await api.post("/auth/logout");
  } catch (e) {
    console.warn("Logout request failed");
  }

  localStorage.removeItem("token");

  return true;
});

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setAuth: (state, action) => {
      state.user = action.payload.user;
      state.token = action.payload.token;
    },
    logout: (state) => {
      state.user = null;
      state.token = null;
    },
  },
  extraReducers: (builder) => {
    builder.addCase(logoutUser.fulfilled, (state) => {
      state.user = null;
      state.token = null;
    });
  },
});

export const { setAuth, logout } = authSlice.actions;
export default authSlice.reducer;
