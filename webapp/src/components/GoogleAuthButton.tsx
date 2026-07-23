import { GoogleLogin, type CredentialResponse } from "@react-oauth/google";
import { Box } from "@mui/material";

type Props = {
  onSuccess: (credentialResponse: CredentialResponse) => void;
  onError?: () => void;
};

export default function GoogleAuthButton({ onSuccess, onError }: Props) {
  return (
    <Box
      sx={{
        width: "100%",
        minHeight: 44,
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        lineHeight: 0,
      }}
    >
      <GoogleLogin
        onSuccess={onSuccess}
        onError={onError}
        shape="pill"
        size="large"
        text="continue_with"
        logo_alignment="left"
        width="400"
      />
    </Box>
  );
}
