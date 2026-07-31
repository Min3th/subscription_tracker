import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Stack,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import type { AppDispatch, RootState } from "../app/store";
import SuggestionReviewDialog from "../components/SuggestionReviewDialog";
import GmailVerificationCard from "../components/GmailVerificationCard";
import {
  completeGmailVerification,
  fetchSuggestions,
  ignoreSuggestion,
} from "../features/suggestions/suggestionsSlice";
import type { SubscriptionSuggestion } from "../types/suggestion";
import { useSnackbar } from "../utils/Snackbar";

export default function Suggestions() {
  const { t } = useTranslation();
  const dispatch = useDispatch<AppDispatch>();
  const snackbar = useSnackbar();
  const { items, status, decidingId, error } = useSelector(
    (state: RootState) => state.suggestions,
  );
  const [reviewing, setReviewing] = useState<SubscriptionSuggestion | null>(null);
  const [ignoring, setIgnoring] = useState<SubscriptionSuggestion | null>(null);

  useEffect(() => {
    dispatch(fetchSuggestions());
  }, [dispatch]);

  const confirmIgnore = async () => {
    if (!ignoring) return;
    try {
      await dispatch(ignoreSuggestion(ignoring.id)).unwrap();
      snackbar.success(t("suggestions.ignored", "Suggestion ignored"));
      setIgnoring(null);
    } catch {
      snackbar.error(t("suggestions.ignore_error", "We couldn't ignore this suggestion. Please try again."));
    }
  };

  const completeVerification = async (suggestion: SubscriptionSuggestion) => {
    try {
      await dispatch(completeGmailVerification(suggestion.id)).unwrap();
      snackbar.success(t("suggestions.gmail_completed", "Gmail forwarding verified"));
    } catch {
      snackbar.error(t("suggestions.gmail_complete_error", "We couldn't save the verification step. Please try again."));
    }
  };

  return (
    <Box sx={{ width: "100%", maxWidth: 1100, py: 4 }}>
      <Stack spacing={1} sx={{ mb: 3 }}>
        <Typography component="h1" variant="h4">
          {t("suggestions.title", "Review suggestions")}
        </Typography>
        <Typography color="text.secondary">
          {t("suggestions.subtitle", "Nothing is added until you review and confirm it.")}
        </Typography>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {status === "loading" && (
        <Box role="status" aria-label={t("suggestions.loading", "Loading suggestions")} sx={{ py: 8, textAlign: "center" }}>
          <CircularProgress />
        </Box>
      )}
      {status !== "loading" && items.length === 0 && (
        <Card variant="outlined">
          <CardContent sx={{ py: 6, textAlign: "center" }}>
            <Typography variant="h6">{t("suggestions.empty_title", "You're all caught up")}</Typography>
            <Typography color="text.secondary">
              {t("suggestions.empty_body", "Forwarded subscription emails will appear here for review.")}
            </Typography>
          </CardContent>
        </Card>
      )}
      <Stack spacing={2}>
        {items.map((suggestion) => {
          const busy = decidingId === suggestion.id;
          if (suggestion.eventType === "GMAIL_VERIFICATION") {
            return (
              <GmailVerificationCard
                key={suggestion.id}
                suggestion={suggestion}
                busy={busy}
                onComplete={() => completeVerification(suggestion)}
                onIgnore={() => setIgnoring(suggestion)}
              />
            );
          }
          return (
            <Card key={suggestion.id} variant="outlined">
              <CardContent>
                <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={2}>
                  <Box>
                    <Typography component="h2" variant="h6">
                      {suggestion.planName
                        ? `${suggestion.provider} — ${suggestion.planName}`
                        : suggestion.provider}
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                      {suggestion.evidenceSummary}
                    </Typography>
                  </Box>
                  <Stack direction="row" spacing={1} alignItems="flex-start">
                    <Chip label={suggestion.eventType.replaceAll("_", " ")} size="small" />
                    <Chip
                      label={t("suggestions.confidence", {
                        defaultValue: "{{value}}% confidence",
                        value: Math.round(suggestion.confidence * 100),
                      })}
                      size="small"
                      variant="outlined"
                    />
                  </Stack>
                </Stack>
                <Stack direction="row" spacing={3} useFlexGap flexWrap="wrap" sx={{ mt: 2 }}>
                  <Typography>
                    {suggestion.amount === null || !suggestion.currency
                      ? t("suggestions.amount_unknown", "Amount needs review")
                      : new Intl.NumberFormat(undefined, {
                          style: "currency",
                          currency: suggestion.currency,
                        }).format(suggestion.amount)}
                  </Typography>
                  {suggestion.billingIntervalUnit && (
                    <Typography color="text.secondary">
                      {t("suggestions.billing_interval", {
                        defaultValue: "Every {{count}} {{unit}}",
                        count: suggestion.billingIntervalCount ?? 1,
                        unit: suggestion.billingIntervalUnit,
                      })}
                    </Typography>
                  )}
                  <Typography color="text.secondary">
                    {new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(
                      new Date(suggestion.receivedAt),
                    )}
                  </Typography>
                </Stack>
                {suggestion.possibleDuplicate && (
                  <Alert severity="warning" sx={{ mt: 2 }}>
                    {t("suggestions.possible_duplicate", {
                      defaultValue: "Possible duplicate: {{name}}",
                      name: suggestion.possibleDuplicate.name,
                    })}
                  </Alert>
                )}
              </CardContent>
              <CardActions sx={{ justifyContent: "flex-end", px: 2, pb: 2 }}>
                <Button color="inherit" onClick={() => setIgnoring(suggestion)} disabled={busy}>
                  {t("suggestions.ignore", "Ignore")}
                </Button>
                <Button variant="contained" onClick={() => setReviewing(suggestion)} disabled={busy}>
                  {t("suggestions.review", "Review and confirm")}
                </Button>
              </CardActions>
            </Card>
          );
        })}
      </Stack>

      <SuggestionReviewDialog
        suggestion={reviewing}
        open={Boolean(reviewing)}
        onClose={() => setReviewing(null)}
      />
      <Dialog open={Boolean(ignoring)} onClose={() => setIgnoring(null)}>
        <DialogTitle>{t("suggestions.ignore_title", "Ignore this suggestion?")}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t("suggestions.ignore_body", "It won't create or change a subscription.")}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIgnoring(null)} disabled={decidingId === ignoring?.id}>
            {t("suggestions.cancel", "Cancel")}
          </Button>
          <Button color="error" onClick={confirmIgnore} disabled={decidingId === ignoring?.id}>
            {t("suggestions.ignore", "Ignore")}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
