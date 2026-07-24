import { Suspense, useState } from "react";
import Navbar from "../components/Navbar";
import { Outlet } from "react-router-dom";
import { Box, Toolbar } from "@mui/material";
import Footer from "../components/Footer";
import SideDrawer from "../components/SideDrawer";

export default function DashboardLayout() {
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
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
    </Box>
  );
}
