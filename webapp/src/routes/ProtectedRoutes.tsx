import { Navigate } from "react-router-dom";
import { CircularProgress, Box } from "@mui/material";
import { useSelector } from "react-redux";
import type { RootState } from "../app/store";

type Props = {
  children: React.ReactNode;
};

export default function ProtectedRoute({ children }: Props) {
  const { token, sessionStatus } = useSelector((state: RootState) => state.auth);
  if (sessionStatus !== "ready") {
    return (
      <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    );
  }
  if (!token) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
