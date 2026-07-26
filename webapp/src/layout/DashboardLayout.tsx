import { Suspense, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import Navbar from "../components/Navbar";
import { Outlet } from "react-router-dom";
import { Box, Toolbar } from "@mui/material";
import Footer from "../components/Footer";
import SideDrawer from "../components/SideDrawer";
import WelcomeGuide from "../components/WelcomeGuide";
import type { AppDispatch, RootState } from "../app/store";
import { updatePreferences } from "../features/preferences/preferencesSlice";
import { useSnackbar } from "../utils/Snackbar";

export default function DashboardLayout() {
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [onboardingDismissed, setOnboardingDismissed] = useState(false);
  const dispatch = useDispatch<AppDispatch>();
  const snackbar = useSnackbar();
  const preferences = useSelector((state: RootState) => state.preferences);
  const showWelcomeGuide =
    preferences.status === "succeeded"
    && !preferences.onboardingCompleted
    && !onboardingDismissed;

  const completeOnboarding = async (action: "dismiss" | "add-subscription") => {
    setOnboardingDismissed(true);

    try {
      await dispatch(updatePreferences({ onboardingCompleted: true })).unwrap();
    } catch {
      snackbar.error("We couldn't save your welcome guide progress. You can continue using Subtrak.");
    }

    if (action === "add-subscription") {
      window.dispatchEvent(new Event("open_add_subscription"));
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
      }}
    >
      <SideDrawer
        mobileOpen={mobileDrawerOpen}
        onMobileClose={() => setMobileDrawerOpen(false)}
      />
      <Box sx={{ flex: 1, minWidth: 0, display: "flex", flexDirection: "column" }}>
        <Navbar
          onClick={() => setMobileDrawerOpen(true)}
          open={false}
          showDrawerButton={true}
        />
        <Toolbar />
        <Box
          component="main"
          sx={{
            flex: 1,
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            flexDirection: "column",
            px: 2,
          }}
        >
          <Suspense fallback={null}>
            <Outlet />
          </Suspense>
        </Box>
        <Footer />
      </Box>
      <WelcomeGuide open={showWelcomeGuide} onComplete={completeOnboarding} />
    </Box>
  );
}
