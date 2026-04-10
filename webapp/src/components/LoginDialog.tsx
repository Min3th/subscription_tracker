import { Dialog, DialogTitle, DialogContent, Box, Typography } from "@mui/material";
import GoogleAuthButton from "./GoogleAuthButton";
import { jwtDecode } from "jwt-decode";

type Props = {
  open: boolean;
  onClose: () => void;
};

export default function LoginDialog({ open, onClose }: Props) {
  const handleGoogleSuccess = (credentialResponse: any) => {
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
