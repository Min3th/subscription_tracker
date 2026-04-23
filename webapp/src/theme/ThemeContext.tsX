import { createContext, useMemo } from "react";
import { ThemeProvider, CssBaseline, useMediaQuery } from "@mui/material";
import { getTheme } from "./theme";
import type { ReactNode } from "react";
import { useSelector, useDispatch } from "react-redux";
import type { RootState, AppDispatch } from "../app/store";
import { setPreferences, updatePreferences } from "../features/preferences/preferencesSlice";

type ColorModeContextType = {
  mode: "light" | "dark";
  setMode: (mode: "light" | "dark" | "auto") => void;
  toggleColorMode: () => void;
};

export const ColorModeContext = createContext<ColorModeContextType>({
  mode: "light",
  setMode: () => {},
  toggleColorMode: () => {},
});

export default function ThemeContextProvider({ children }: { children: ReactNode }) {
  const token = localStorage.getItem("token");

  const dispatch = useDispatch<AppDispatch>();
  const preferenceTheme = useSelector((state: RootState) => state.preferences.theme);

  const prefersDarkMode = useMediaQuery("(prefers-color-scheme: dark)");

  const computedMode = useMemo(() => {
    if (preferenceTheme === "auto") {
      return prefersDarkMode ? "dark" : "light";
    }
    return (preferenceTheme as "light" | "dark") || "light";
  }, [preferenceTheme, prefersDarkMode]);

  const colorMode = useMemo(
    () => ({
      mode: computedMode,
      setMode: (mode: "light" | "dark" | "auto") => {
        dispatch(setPreferences({ theme: mode }));
        if (token) {
          dispatch(updatePreferences({ theme: mode }));
        }
      },
      toggleColorMode: () => {
        const nextTheme = computedMode === "light" ? "dark" : "light";
        dispatch(setPreferences({ theme: nextTheme }));
        if (token) {
          dispatch(updatePreferences({ theme: nextTheme }));
        }
      },
    }),
    [computedMode, dispatch],
  );

  const theme = useMemo(() => getTheme(computedMode), [computedMode]);

  return (
    <ColorModeContext.Provider value={colorMode}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
}
