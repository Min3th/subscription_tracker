import {
  Alert,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import { useTranslation } from "react-i18next";
import type { SubscriptionSuggestion } from "../types/suggestion";

interface Props {
  suggestion: SubscriptionSuggestion;
  busy: boolean;
  onComplete: () => void;
  onIgnore: () => void;
}

export const trustedGmailVerificationUrl = (value: string | null): string | null => {
  if (!value || value.length > 2000) return null;
  try {
    const url = new URL(value);
    return url.protocol === "https:"
      && url.hostname === "mail-settings.google.com"
      && url.port === ""
      && url.username === ""
      && url.password === ""
      && url.pathname.startsWith("/mail/vf-")
      ? url.toString()
      : null;
  } catch {
    return null;
  }
};

export default function GmailVerificationCard({
  suggestion,
  busy,
  onComplete,
  onIgnore,
}: Props) {
  const { t } = useTranslation();
  const actionUrl = trustedGmailVerificationUrl(suggestion.actionUrl);

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={2}>
          <div>
            <Typography component="h2" variant="h6">
              {t("suggestions.gmail_title", "Complete Gmail forwarding verification")}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 0.5 }}>
              {t(
                "suggestions.gmail_body",
                "Gmail sent a verification request for your Subtrak forwarding address.",
              )}
            </Typography>
          </div>
          <Chip label={t("suggestions.gmail_setup", "Gmail setup")} color="info" size="small" />
        </Stack>
        <Alert severity={actionUrl ? "info" : "warning"} sx={{ mt: 2 }}>
          {actionUrl
            ? t(
                "suggestions.gmail_security",
                "Check that the next page is a Google page, complete verification there, then return here.",
              )
            : t(
                "suggestions.gmail_link_missing",
                "The verification message arrived, but it did not contain a trusted Google verification link. Try sending a new request from Gmail.",
              )}
        </Alert>
      </CardContent>
      <CardActions sx={{ justifyContent: "flex-end", px: 2, pb: 2, flexWrap: "wrap" }}>
        <Button color="inherit" onClick={onIgnore} disabled={busy}>
          {t("suggestions.ignore", "Ignore")}
        </Button>
        {actionUrl && (
          <Button
            component="a"
            href={actionUrl}
            target="_blank"
            rel="noopener noreferrer"
            variant="outlined"
            endIcon={<OpenInNewIcon />}
          >
            {t("suggestions.gmail_open", "Open Google verification")}
          </Button>
        )}
        <Button variant="contained" onClick={onComplete} disabled={busy || !actionUrl}>
          {busy
            ? t("suggestions.gmail_completing", "Saving…")
            : t("suggestions.gmail_complete", "I've completed verification")}
        </Button>
      </CardActions>
    </Card>
  );
}
