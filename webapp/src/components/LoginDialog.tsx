import { Dialog, DialogTitle, DialogContent, Box, Typography, Stack } from "@mui/material";
import GoogleAuthButton from "./GoogleAuthButton";
import { jwtDecode } from "jwt-decode";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { setAuth } from "../app/authSlice";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import api from "../api/client";

type Props = {
  open: boolean;
  onClose: () => void;
};

export default function LoginDialog({ open, onClose }: Props) {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const handleGoogleSuccess = async (credentialResponse: any) => {
    try {
      const res = await api.post("/auth/google", {
        credential: credentialResponse.credential,
      });

      const user: any = jwtDecode(credentialResponse.credential);
      dispatch(
        setAuth({
          user,
          token: res.data.accessToken,
        }),
      );
      localStorage.setItem("token", res.data.accessToken);
      localStorage.setItem("user", JSON.stringify(user));
      navigate("/dashboard");
    } catch (error) {
      console.error("Error decoding JWT:", error);
    }
    if (credentialResponse.credential) {
      const user = jwtDecode(credentialResponse.credential);
      console.log(user);
    }
    console.log(credentialResponse.credential);
  };
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogContent>
        <Box
          sx={{
            p: 4,
            width: 400,
            borderRadius: 4,
            textAlign: "center",
          }}
        >
          {/* ICON */}
          <Box
            sx={{
              width: 64,
              height: 64,
              mx: "auto",
              mb: 2,
              borderRadius: "50%",
              background: "linear-gradient(135deg, #7C3AED, #9333EA)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <CreditCardIcon sx={{ color: "white", fontSize: 32 }} />
          </Box>

          {/* TITLE */}
          <Typography variant="h5" fontWeight="bold" mb={1}>
            Subscription Tracker
          </Typography>

          <Typography variant="body2" color="text.secondary" mb={3}>
            Manage all your subscriptions in one placesdsas
          </Typography>

          {/* GOOGLE BUTTON */}
          <Box mb={3}>
            <GoogleAuthButton onSuccess={handleGoogleSuccess} />
          </Box>

          {/* FEATURES */}
          <Stack spacing={1.5} alignItems="flex-start" mb={3}>
            {[
              "Track all your subscriptions in one place",
              "Get notified before billing dates",
              "Visualize your spending trends",
            ].map((text) => (
              <Box key={text} display="flex" alignItems="center" gap={1}>
                <CheckCircleIcon sx={{ color: "#22C55E", fontSize: 18 }} />
                <Typography variant="body2">{text}</Typography>
              </Box>
            ))}
          </Stack>

          {/* FOOTER */}
          <Typography variant="caption" color="text.secondary">
            By continuing, you agree to our Terms of Service and Privacy Policy
          </Typography>
        </Box>
      </DialogContent>
    </Dialog>
  );
}
