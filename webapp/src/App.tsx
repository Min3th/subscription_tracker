import { useEffect, useState } from "react";
import "./App.css";
import { getTheme } from "./theme/theme";
import { Provider, useDispatch } from "react-redux";
import { store } from "./app/store.ts";
import { SnackbarProvider } from "./utils/Snackbar.tsx";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import React from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import HomePage from "./pages/HomePage.tsx";
import ThemeContextProvider from "./theme/ThemeContext.tsx";
import { LoaderProvider } from "./utils/Loading.tsx";
import PageTwo from "./pages/PageTwo.tsx";
import PageOne from "./pages/PageOne.tsx";
import DashboardLayout from "./layout/DashboardLayout.tsx";
import { GoogleOAuthProvider } from "@react-oauth/google";
import Dashboard from "./pages/Dashboard.tsx";
import ProtectedRoute from "./routes/ProtectedRoutes.tsx";
import PublicLayout from "./layout/PublicLayout.tsx";
import { setAuth } from "./app/authSlice.ts";

function App() {
  const [mode, setMode] = useState<"light" | "dark">("light");
  const theme = React.useMemo(() => getTheme(mode), [mode]);
  const dispatch = useDispatch();

  useEffect(() => {
    const user = localStorage.getItem("user");
    const token = localStorage.getItem("token");

    if (user && token) {
      dispatch(
        setAuth({
          user: JSON.parse(user),
          token,
        }),
      );
    }
  }, []);
  return (
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
      <LoaderProvider>
        <SnackbarProvider>
          <Provider store={store}>
            <ThemeProvider theme={theme}>
              <ThemeContextProvider>
                <CssBaseline />
                <BrowserRouter>
                  <Routes>
                    <Route element={<PublicLayout />}>
                      <Route path="/" element={<HomePage />} />
                      <Route path="/page1" element={<PageOne />} />
                      <Route path="/page2" element={<PageTwo />} />
                    </Route>
                    <Route element={<DashboardLayout />}>
                      <Route
                        path="/dashboard"
                        element={
                          // <ProtectedRoute>
                          <Dashboard />
                          // </ProtectedRoute>
                        }
                      />
                    </Route>
                  </Routes>
                </BrowserRouter>
              </ThemeContextProvider>
            </ThemeProvider>
          </Provider>
        </SnackbarProvider>
      </LoaderProvider>
    </GoogleOAuthProvider>
  );
}

export default App;
