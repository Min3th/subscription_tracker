import { createTheme } from "@mui/material";
import type { PaletteMode } from "@mui/material";

export const getDesignTokens = (mode: PaletteMode) => ({
  palette: {
    mode,

    ...(mode === "light"
      ? {
          primary: { main: "#6400ef" },
          background: {
            default: "#f5f5f5",
            paper: "#ffffff",
          },
        }
      : {
          primary: { main: "#6400ef" },
          background: {
            default: "#121212",
            paper: "#1e1e1e",
          },
        }),
  },
});

export const getTheme = (mode: PaletteMode) => createTheme(getDesignTokens(mode));
