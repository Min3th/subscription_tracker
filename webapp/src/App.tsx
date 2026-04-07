import { useState } from "react";
import "./App.css";
import { getTheme } from "./theme/theme";
import { Provider } from "react-redux";
import { store } from "./app/store.ts";
import { SnackbarProvider } from "./utils/Snackbar.tsx";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import React from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import HomePage from "./pages/HomePage.tsx";
import ThemeContextProvider from "./theme/ThemeContext.tsx";
import { LoaderProvider } from "./utils/Loading.tsx";
import PageTwo from "./pages/PageTwo.tsx";
import PageOne from "./pages/PageOne.tsx";
import Layout from "./layout/Layout.tsx";

function App() {
  const [mode, setMode] = useState<"light" | "dark">("light");
  const theme = React.useMemo(() => getTheme(mode), [mode]);

  return (
    <LoaderProvider>
      <SnackbarProvider>
        <Provider store={store}>
          <ThemeProvider theme={theme}>
            <ThemeContextProvider>
              <CssBaseline />
              <BrowserRouter>
                <Routes>
                  <Route element={<Layout />}>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/page1" element={<PageOne />} />
                    <Route path="/page2" element={<PageTwo />} />
                  </Route>
                </Routes>
              </BrowserRouter>
            </ThemeContextProvider>
          </ThemeProvider>
        </Provider>
      </SnackbarProvider>
    </LoaderProvider>
  );
}

export default App;
