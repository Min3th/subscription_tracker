import { useState } from "react";
import {
  Alert,
  Box,
  CircularProgress,
  Dialog,
  DialogContent,
  IconButton,
  Stack,
  Typography,
  alpha,
  useTheme,
} from "@mui/material";
import GoogleAuthButton from "./GoogleAuthButton";
import type { CredentialResponse } from "@react-oauth/google";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { setAuth } from "../app/authSlice";
import CloseIcon from "@mui/icons-material/Close";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import api from "../api/client";
import type { AppDispatch } from "../app/store";

type Props = {
  open: boolean;
  onClose: () => void;
};

export default function LoginDialog({ open, onClose }: Props) {
  const theme = useTheme();
  const navigate = useNavigate();
  const dispatch = useDispatch<AppDispatch>();
  const [isSigningIn, setIsSigningIn] = useState(false);
  const [error, setError] = useState("");

  const handleGoogleSuccess = async (credentialResponse: CredentialResponse) => {
    setError("");
    setIsSigningIn(true);
    try {
      if (!credentialResponse.credential) {
        throw new Error("Google did not return an ID credential");
      }
      const res = await api.post("/auth/google", {
        credential: credentialResponse.credential,
      });

      dispatch(
        setAuth({
          user: res.data.user,
          token: res.data.accessToken,
        }),
      );
      onClose();
      navigate("/dashboard");
    } catch (error) {
      console.error("Google sign-in failed:", error);
      setError("We couldn't sign you in. Please try again.");
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleClose = () => {
    if (!isSigningIn) {
      setError("");
      onClose();
    }
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      fullWidth
      maxWidth="sm"
      slotProps={{
        backdrop: {
          sx: {
            backgroundColor: "rgba(17, 8, 35, 0.58)",
            backdropFilter: "blur(6px)",
          },
        },
        paper: {
          sx: {
            overflow: "hidden",
            maxWidth: 500,
            borderRadius: { xs: 4, sm: 5 },
            border: `1px solid ${alpha(theme.palette.primary.main, 0.16)}`,
            backgroundImage: "none",
            boxShadow:
              theme.palette.mode === "dark"
                ? "0 28px 80px rgba(0,0,0,0.58)"
                : "0 28px 80px rgba(55, 20, 100, 0.24)",
            m: 2,
          },
        },
      }}
    >
      <DialogContent sx={{ p: 0, position: "relative" }}>
        <Box
          sx={{
            height: 7,
            background: "linear-gradient(90deg, #6400ef 0%, #a840dc 52%, #d880f1 100%)",
          }}
        />
        <IconButton
          aria-label="Close sign in"
          onClick={handleClose}
          disabled={isSigningIn}
          sx={{
            position: "absolute",
            top: 19,
            right: 16,
            zIndex: 2,
            color: "text.secondary",
            bgcolor: alpha(theme.palette.text.primary, 0.05),
            "&:hover": { bgcolor: alpha(theme.palette.primary.main, 0.1), color: "primary.main" },
          }}
        >
          <CloseIcon fontSize="small" />
        </IconButton>

        <Box
          sx={{
            px: { xs: 3, sm: 5 },
            pt: { xs: 4.5, sm: 5 },
            pb: { xs: 3.5, sm: 4.5 },
            textAlign: "center",
            position: "relative",
            "&::before": {
              content: '""',
              position: "absolute",
              width: 190,
              height: 190,
              borderRadius: "50%",
              top: -105,
              left: -100,
              background: alpha("#6400ef", theme.palette.mode === "dark" ? 0.18 : 0.08),
              filter: "blur(2px)",
              pointerEvents: "none",
            },
          }}
        >
          <Box
            sx={{
              width: 68,
              height: 68,
              mx: "auto",
              mb: 2.5,
              borderRadius: 3.5,
              background: "linear-gradient(135deg, #6400ef, #d880f1)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              transform: "rotate(-4deg)",
              boxShadow: "0 12px 28px rgba(100, 0, 239, 0.3)",
            }}
          >
            <AutoAwesomeIcon sx={{ color: "white", fontSize: 32, transform: "rotate(4deg)" }} />
          </Box>

          <Typography variant="h4" sx={{ fontWeight: 800, mb: 1, fontSize: { xs: "1.75rem", sm: "2rem" } }}>
            Welcome to{" "}
            <Box component="span" sx={{ color: "primary.main" }}>
              Subtrak
            </Box>
          </Typography>

          <Typography variant="body1" color="text.secondary" sx={{ mb: 3.5, lineHeight: 1.65 }}>
            Sign in to track subscriptions, plan your spending, and never miss a renewal.
          </Typography>

          <Box
            sx={{
              minHeight: 44,
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              mb: error ? 2 : 3.5,
              opacity: isSigningIn ? 0.45 : 1,
              pointerEvents: isSigningIn ? "none" : "auto",
              transition: "opacity 0.2s ease",
            }}
          >
            {isSigningIn ? (
              <Stack direction="row" spacing={1.5} alignItems="center">
                <CircularProgress size={22} />
                <Typography variant="body2" color="text.secondary">
                  Signing you in…
                </Typography>
              </Stack>
            ) : (
              <GoogleAuthButton
                onSuccess={handleGoogleSuccess}
                onError={() => setError("Google sign-in was cancelled or unavailable. Please try again.")}
              />
            )}
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 3, textAlign: "left", borderRadius: 2.5 }}>
              {error}
            </Alert>
          )}

          <Stack
            spacing={1.25}
            sx={{
              p: 2.25,
              mb: 3,
              borderRadius: 3,
              bgcolor: alpha(theme.palette.primary.main, theme.palette.mode === "dark" ? 0.1 : 0.045),
              border: `1px solid ${alpha(theme.palette.primary.main, 0.11)}`,
            }}
          >
            {[
              "Track all your subscriptions in one place",
              "Get notified before billing dates",
              "Visualize your spending trends",
            ].map((text) => (
              <Box key={text} display="flex" alignItems="center" gap={1.25}>
                <CheckCircleIcon sx={{ color: "primary.main", fontSize: 19 }} />
                <Typography variant="body2" sx={{ color: "text.primary", fontWeight: 500, textAlign: "left" }}>
                  {text}
                </Typography>
              </Box>
            ))}
          </Stack>

          <Stack direction="row" spacing={0.75} justifyContent="center" alignItems="center">
            <LockOutlinedIcon sx={{ fontSize: 14, color: "text.disabled" }} />
            <Typography variant="caption" color="text.secondary">
              Secure Google sign-in. We never see your password.
            </Typography>
          </Stack>
        </Box>
      </DialogContent>
    </Dialog>
  );
}
