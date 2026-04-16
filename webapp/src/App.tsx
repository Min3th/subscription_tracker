import "./App.css";
import { Provider } from "react-redux";
import { store } from "./app/store.ts";
import { SnackbarProvider } from "./utils/Snackbar.tsx";
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
import AppContent from "./AppContent.tsx";
import Subscriptions from "./pages/Subscriptions.tsx";
import UserProfile from "./pages/UserProfile.tsx";
import { Settings } from "./pages/Settings.tsx";

function App() {
  return (
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
      <LoaderProvider>
        <SnackbarProvider>
          <Provider store={store}>
            <AppContent>
              <ThemeContextProvider>
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
                      <Route
                        path="/subscriptions"
                        element={
                          // <ProtectedRoute>
                          <Subscriptions />
                          // </ProtectedRoute>
                        }
                      />
                      <Route
                        path="/settings"
                        element={
                          // <ProtectedRoute>
                          <Settings />
                          // </ProtectedRoute>
                        }
                      />
                    </Route>
                  </Routes>
                </BrowserRouter>
              </ThemeContextProvider>
            </AppContent>
          </Provider>
        </SnackbarProvider>
      </LoaderProvider>
    </GoogleOAuthProvider>
  );
}

export default App;
