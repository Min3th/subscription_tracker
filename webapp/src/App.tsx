import "./App.css";
import { lazy, Suspense } from "react";
import { Provider } from "react-redux";
import { store } from "./app/store.ts";
import { SnackbarProvider } from "./utils/Snackbar.tsx";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import ThemeContextProvider from "./theme/ThemeContext.tsx";
import { LoaderProvider } from "./utils/Loading.tsx";
import { GoogleOAuthProvider } from "@react-oauth/google";
import AppContent from "./AppContent.tsx";
import Loading from "./utils/Loading.tsx";

const HomePage = lazy(() => import("./pages/HomePage.tsx"));
const DashboardLayout = lazy(() => import("./layout/DashboardLayout.tsx"));
const Dashboard = lazy(() => import("./pages/Dashboard.tsx"));
const ProtectedRoute = lazy(() => import("./routes/ProtectedRoutes.tsx"));
const PublicLayout = lazy(() => import("./layout/PublicLayout.tsx"));
const Subscriptions = lazy(() => import("./pages/Subscriptions.tsx"));
const Settings = lazy(() =>
  import("./pages/Settings.tsx").then(({ Settings }) => ({
    default: Settings,
  })),
);

const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    children: [{ path: "/", element: <HomePage /> }],
  },
  {
    element: (
      <ProtectedRoute>
        <DashboardLayout />
      </ProtectedRoute>
    ),
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
                <Suspense fallback={<Loading />}>
                  <RouterProvider router={router} />
                </Suspense>
              </ThemeContextProvider>
            </AppContent>
          </Provider>
        </SnackbarProvider>
      </LoaderProvider>
    </GoogleOAuthProvider>
  );
}

export default App;
