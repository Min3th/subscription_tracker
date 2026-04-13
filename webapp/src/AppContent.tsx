import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { setAuth } from "./app/authSlice";

export default function AppContent({ children }: { children: React.ReactNode }) {
  const dispatch = useDispatch();

  useEffect(() => {
    const user = localStorage.getItem("user");
    const token = localStorage.getItem("token");

    if (user && token) {
      dispatch(
        setAuth({
          user: JSON.parse(user),
          token,
        }),
      );
    }
  }, [dispatch]);

  return <>{children}</>;
}
