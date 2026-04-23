import { Suspense, useState } from "react";
import Navbar from "../components/Navbar";
import { Outlet } from "react-router-dom";
import { Box, Toolbar } from "@mui/material";
import Footer from "../components/Footer";
import ThemeToggleButton from "../components/ThemeToggleButton";

export default function PublicLayout() {
  const [open, setOpen] = useState(false);
  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
      }}
    >
      <Box sx={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <Navbar onClick={() => setOpen(true)} open={open} />
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
        {/* <ThemeToggleButton /> */}
        <Footer />
      </Box>
    </Box>
  );
}
