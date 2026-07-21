import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { initializeSession } from "./app/authSlice";
import { fetchPreferences } from "./features/preferences/preferencesSlice";
import type { RootState, AppDispatch } from "./app/store";
import { useTranslation } from "react-i18next";

export default function AppContent({ children }: { children: React.ReactNode }) {
  const dispatch = useDispatch<AppDispatch>();
  const { i18n } = useTranslation();
  const language = useSelector((state: RootState) => state.preferences.language);
  const user = useSelector((state: RootState) => state.auth.user);

  useEffect(() => {
    dispatch(initializeSession());
  }, [dispatch]);

  useEffect(() => {
    if (user) {
      dispatch(fetchPreferences());
    }
  }, [dispatch, user]);

  useEffect(() => {
    if (language) {
      i18n.changeLanguage(language);
    }
  }, [language, i18n]);

  return <>{children}</>;
}
