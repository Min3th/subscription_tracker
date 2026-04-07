import { useContext } from "react";
import { IconButton } from "@mui/material";
import LightModeIcon from "@mui/icons-material/LightMode";
import DarkModeIcon from "@mui/icons-material/DarkMode";
import { useTheme } from "@mui/material/styles";
import { ColorModeContext } from "../theme/ThemeContext";
import { Box } from "@mui/material";
import { Fab } from "@mui/material";

export default function ThemeToggleButton() {
  const theme = useTheme();
  const colorMode = useContext(ColorModeContext);

  return (
    <Box
      sx={{
        position: "fixed",
        bottom: 20,
        left: 20,
        zIndex: 1300,
      }}
    >
      <Fab
        color="primary"
        onClick={colorMode.toggleColorMode}
        sx={{
          position: "fixed",
          bottom: 20,
          left: 20,
          zIndex: 1300,
        }}
      >
        {theme.palette.mode === "dark" ? <LightModeIcon /> : <DarkModeIcon />}
      </Fab>
    </Box>
  );
}
