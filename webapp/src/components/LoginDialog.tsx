import { Dialog, DialogTitle, DialogContent, Box, Typography } from "@mui/material";
import GoogleAuthButton from "./GoogleAuthButton";
import { jwtDecode } from "jwt-decode";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { setAuth } from "../app/authSlice";

type Props = {
  open: boolean;
  onClose: () => void;
};

export default function LoginDialog({ open, onClose }: Props) {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const handleGoogleSuccess = async (credentialResponse: any) => {
    try {
      const res = await axios.post("http://localhost:8080/auth/google", {
        credential: credentialResponse.credential,
      });

      const user: any = jwtDecode(credentialResponse.credential);
      dispatch(
        setAuth({
          user,
          token: res.data.token,
        }),
      );
      localStorage.setItem("token", res.data.token);
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
      <DialogTitle
        sx={{
          textAlign: "center",
          fontWeight: 700,
          color: "#6400ef",
        }}
      >
        Welcome Back 👋
      </DialogTitle>

      <DialogContent>
        <Box
          sx={{
            py: 2,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: 2,
            minWidth: "300px",
          }}
        >
          <Typography variant="body2" color="text.secondary">
            Sign in to continue
          </Typography>

          <GoogleAuthButton onSuccess={handleGoogleSuccess} />

          <Typography variant="caption" color="text.secondary">
            Secure login powered by Google
          </Typography>
        </Box>
      </DialogContent>
    </Dialog>
  );
}
