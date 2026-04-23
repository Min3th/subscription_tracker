import { GoogleLogin } from "@react-oauth/google";

type Props = {
  onSuccess: (credentialResponse: any) => void;
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
