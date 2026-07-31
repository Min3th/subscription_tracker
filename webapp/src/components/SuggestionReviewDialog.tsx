import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Stack,
  Switch,
  TextField,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import type { AppDispatch } from "../app/store";
import { confirmSuggestion } from "../features/suggestions/suggestionsSlice";
import {
  SUBSCRIPTION_CATEGORIES,
  type BillingUnit,
  type SubscriptionCategory,
} from "../types/subscription";
import type { SubscriptionSuggestion } from "../types/suggestion";
import { SUPPORTED_CURRENCIES } from "../utils/money";
import { useSnackbar } from "../utils/Snackbar";

interface Props {
  suggestion: SubscriptionSuggestion | null;
  open: boolean;
  onClose: () => void;
}

interface FormState {
  name: string;
  cost: string;
  currency: string;
  category: SubscriptionCategory;
  startDate: string;
  billingIntervalUnit: BillingUnit;
  billingIntervalCount: string;
  description: string;
  emailNotificationsEnabled: boolean;
}

const initialForm = (suggestion: SubscriptionSuggestion): FormState => ({
  name: suggestion.planName
    ? `${suggestion.provider} ${suggestion.planName}`
    : suggestion.provider,
  cost: suggestion.amount === null ? "" : String(suggestion.amount),
  currency: suggestion.currency ?? "USD",
  category: "Other",
  startDate: suggestion.startDate ?? "",
  billingIntervalUnit: suggestion.billingIntervalUnit ?? "month",
  billingIntervalCount: String(suggestion.billingIntervalCount ?? 1),
  description: suggestion.evidenceSummary,
  emailNotificationsEnabled: false,
});

export default function SuggestionReviewDialog({ suggestion, open, onClose }: Props) {
  const { t } = useTranslation();
  const dispatch = useDispatch<AppDispatch>();
  const snackbar = useSnackbar();
  const [form, setForm] = useState<FormState | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (suggestion && open) setForm(initialForm(suggestion));
  }, [open, suggestion]);

  const errors = useMemo(() => {
    if (!form) return {};
    const amount = Number(form.cost);
    const intervalCount = Number(form.billingIntervalCount);
    return {
      name: form.name.trim() ? "" : t("suggestions.validation_name", "Name is required"),
      cost:
        Number.isFinite(amount) && amount > 0
          ? ""
          : t("suggestions.validation_amount", "Enter a positive amount"),
      interval:
        Number.isInteger(intervalCount) && intervalCount > 0
          ? ""
          : t("suggestions.validation_interval", "Enter a positive whole number"),
      startDate: form.startDate
        ? ""
        : t("suggestions.validation_start_date", "Start date is required"),
    };
  }, [form, t]);

  if (!suggestion || !form) return null;

  const submit = async () => {
    if (errors.name || errors.cost || errors.interval || errors.startDate) return;
    setSubmitting(true);
    try {
      await dispatch(
        confirmSuggestion({
          id: suggestion.id,
          request: {
            name: form.name.trim(),
            cost: Number(form.cost),
            currency: form.currency,
            type: "recurring",
            category: form.category,
            description: form.description.trim() || null,
            startDate: form.startDate,
            billingIntervalUnit: form.billingIntervalUnit,
            billingIntervalCount: Number(form.billingIntervalCount),
            emailNotificationsEnabled: form.emailNotificationsEnabled,
          },
        }),
      ).unwrap();
      snackbar.success(t("suggestions.confirmed", "Subscription added"));
      onClose();
    } catch {
      snackbar.error(t("suggestions.confirm_error", "We couldn't add this subscription. Please try again."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={submitting ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>{t("suggestions.review_title", "Review subscription")}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {suggestion.possibleDuplicate && (
            <Alert severity="warning">
              {t("suggestions.duplicate_warning", {
                defaultValue: "Possible duplicate of {{name}}. Confirm only if this is a separate subscription.",
                name: suggestion.possibleDuplicate.name,
              })}
            </Alert>
          )}
          <TextField
            autoFocus
            required
            label={t("suggestions.name", "Subscription name")}
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            error={Boolean(errors.name)}
            helperText={errors.name}
            inputProps={{ maxLength: 120 }}
          />
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
            <TextField
              required
              fullWidth
              type="number"
              label={t("suggestions.amount", "Amount")}
              value={form.cost}
              onChange={(event) => setForm({ ...form, cost: event.target.value })}
              error={Boolean(errors.cost)}
              helperText={errors.cost}
              inputProps={{ min: 0, step: "0.01" }}
            />
            <TextField
              required
              select
              fullWidth
              label={t("suggestions.currency", "Currency")}
              value={form.currency}
              onChange={(event) => setForm({ ...form, currency: event.target.value })}
            >
              {SUPPORTED_CURRENCIES.map((currency) => (
                <MenuItem key={currency} value={currency}>{currency}</MenuItem>
              ))}
            </TextField>
          </Stack>
          <TextField
            required
            select
            label={t("suggestions.category", "Category")}
            value={form.category}
            onChange={(event) =>
              setForm({ ...form, category: event.target.value as SubscriptionCategory })
            }
          >
            {SUBSCRIPTION_CATEGORIES.map((category) => (
              <MenuItem key={category} value={category}>{category}</MenuItem>
            ))}
          </TextField>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
            <TextField
              required
              fullWidth
              type="number"
              label={t("suggestions.interval_count", "Every")}
              value={form.billingIntervalCount}
              onChange={(event) =>
                setForm({ ...form, billingIntervalCount: event.target.value })
              }
              error={Boolean(errors.interval)}
              helperText={errors.interval}
              inputProps={{ min: 1, step: 1 }}
            />
            <TextField
              required
              select
              fullWidth
              label={t("suggestions.interval_unit", "Billing period")}
              value={form.billingIntervalUnit}
              onChange={(event) =>
                setForm({ ...form, billingIntervalUnit: event.target.value as BillingUnit })
              }
            >
              {(["day", "week", "month", "year"] as const).map((unit) => (
                <MenuItem key={unit} value={unit}>{unit}</MenuItem>
              ))}
            </TextField>
          </Stack>
          <TextField
            required
            type="date"
            label={t("suggestions.start_date", "Start date")}
            value={form.startDate}
            onChange={(event) => setForm({ ...form, startDate: event.target.value })}
            error={Boolean(errors.startDate)}
            helperText={errors.startDate}
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            multiline
            minRows={2}
            label={t("suggestions.notes", "Notes")}
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
            inputProps={{ maxLength: 1000 }}
          />
          <FormControlLabel
            control={
              <Switch
                checked={form.emailNotificationsEnabled}
                onChange={(event) =>
                  setForm({ ...form, emailNotificationsEnabled: event.target.checked })
                }
              />
            }
            label={t("suggestions.reminders", "Email me before renewal")}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={submitting}>
          {t("suggestions.cancel", "Cancel")}
        </Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={submitting || Boolean(errors.name || errors.cost || errors.interval)}
        >
          {submitting
            ? t("suggestions.confirming", "Adding…")
            : t("suggestions.confirm", "Confirm and add")}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
