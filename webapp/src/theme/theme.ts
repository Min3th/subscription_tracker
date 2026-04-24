import { createTheme } from "@mui/material";
import type { PaletteMode } from "@mui/material";

declare module "@mui/material/styles" {
  interface Palette {
    purpink: string;
  }
  interface PaletteOptions {
    purpink?: string;
  }
}

export const getDesignTokens = (mode: PaletteMode) => ({
  palette: {
    mode,
    purpink: mode === "dark" ? "#d880f1" : "#6400ef",

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
