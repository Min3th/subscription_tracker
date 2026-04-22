import { Backdrop, CircularProgress, Typography } from "@mui/material";
import { createContext, useContext, useState, useEffect, type ReactNode } from "react";

interface LoaderContextType {
  showLoader: (message?: string) => void;
  hideLoader: () => void;
}

const LoaderContext = createContext<LoaderContextType | undefined>(undefined);

export const useLoader = () => {
  const context = useContext(LoaderContext);
  if (!context) {
    throw new Error("useLoader must be used within a LoaderProvider");
  }
  return context;
};

export const LoaderProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("Loading...");

  const showLoader = (msg: string = "Loading...") => {
    setMessage(msg);
    setOpen(true);
  };

  const hideLoader = () => {
    setOpen(false);
  };

  useEffect(() => {
    const handleShow = (e: Event) => {
      const customEvent = e as CustomEvent;
      showLoader(customEvent.detail?.message || "Loading...");
    };
    const handleHide = () => hideLoader();

    window.addEventListener("show-loader", handleShow);
    window.addEventListener("hide-loader", handleHide);

    return () => {
      window.removeEventListener("show-loader", handleShow);
      window.removeEventListener("hide-loader", handleHide);
    };
  }, []);

  return (
    <LoaderContext.Provider value={{ showLoader, hideLoader }}>
      {children}
      {open && <Loader message={message} />}
    </LoaderContext.Provider>
  );
};

const Loader = ({ message = "Loading..." }) => {
  return (
    <Backdrop
      open={true}
      sx={{
        color: "#fff",

        backgroundColor: "rgba(0, 0, 0, 0.5)",
        zIndex: (theme) => theme.zIndex.drawer + 1,
        flexDirection: "column",
      }}
    >
      <CircularProgress color="inherit" />
      <Typography variant="h6" sx={{ mt: 2 }}>
        {message}
      </Typography>
    </Backdrop>
  );
};

export default Loader;
