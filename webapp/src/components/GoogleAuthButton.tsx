import { GoogleLogin, type CredentialResponse } from "@react-oauth/google";

type Props = {
  onSuccess: (credentialResponse: CredentialResponse) => void;
};

export default function GoogleAuthButton({ onSuccess }: Props) {
  return (
    <GoogleLogin
      onSuccess={onSuccess}
      onError={() => {
      }}
    />
  );
}
