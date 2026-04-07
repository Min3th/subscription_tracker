import { Suspense } from "react";
import Navbar from "../components/Navbar";
import { Outlet } from "react-router-dom";
import { Box } from "@mui/material";
import Footer from "../components/Footer";
import ThemeToggleButton from "../components/ThemeToggleButton";

export default function Layout() {
  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <Navbar />
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
      <ThemeToggleButton />
      <Footer />
    </Box>
  );
}
