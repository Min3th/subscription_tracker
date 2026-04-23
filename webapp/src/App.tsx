import "./App.css";
import { Provider } from "react-redux";
import { store } from "./app/store.ts";
import { SnackbarProvider } from "./utils/Snackbar.tsx";
import React from "react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
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

const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    children: [
      { path: "/", element: <HomePage /> },
      { path: "/page1", element: <PageOne /> },
      { path: "/page2", element: <PageTwo /> },
    ],
  },
  {
    element: <DashboardLayout />,
    children: [
      {
        path: "/dashboard",
        element: <Dashboard />,
      },
      {
        path: "/subscriptions",
        element: <Subscriptions />,
      },
      {
        path: "/settings",
        element: <Settings />,
      },
    ],
  },
]);

function App() {
  return (
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
      <LoaderProvider>
        <SnackbarProvider>
          <Provider store={store}>
            <AppContent>
              <ThemeContextProvider>
                <RouterProvider router={router} />
              </ThemeContextProvider>
            </AppContent>
          </Provider>
        </SnackbarProvider>
      </LoaderProvider>
    </GoogleOAuthProvider>
  );
}

export default App;
