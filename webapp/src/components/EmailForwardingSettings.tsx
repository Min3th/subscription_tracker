import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField,
  Typography,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import ForwardToInboxIcon from "@mui/icons-material/ForwardToInbox";
import RefreshIcon from "@mui/icons-material/Refresh";
import {
  createInboundEmailAddress,
  getInboundEmailAddress,
  revokeInboundEmailAddress,
  rotateInboundEmailAddress,
  type InboundEmailAddress,
} from "../api/inboundEmail";
import { useTranslation } from "react-i18next";
import { useSnackbar } from "../utils/Snackbar";

type ConfirmationAction = "rotate" | "revoke" | null;

export default function EmailForwardingSettings() {
  const { t } = useTranslation();
  const snackbar = useSnackbar();
  const [address, setAddress] = useState<InboundEmailAddress | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionPending, setActionPending] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [confirmation, setConfirmation] = useState<ConfirmationAction>(null);

  const loadAddress = useCallback(async () => {
    setLoading(true);
    setLoadFailed(false);
    try {
      setAddress(await getInboundEmailAddress());
    } catch {
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAddress();
  }, [loadAddress]);

  const generateAddress = async () => {
    setActionPending(true);
    try {
      setAddress(await createInboundEmailAddress());
      snackbar.success(t("email_forwarding.generated"));
    } catch {
      snackbar.error(t("email_forwarding.action_error"));
    } finally {
      setActionPending(false);
    }
  };

  const copyAddress = async () => {
    if (!address?.address) return;
    try {
      await navigator.clipboard.writeText(address.address);
      snackbar.success(t("email_forwarding.copied"));
    } catch {
      snackbar.error(t("email_forwarding.copy_error"));
    }
  };

  const confirmLifecycleAction = async () => {
    if (!confirmation) return;
    setActionPending(true);
    try {
      if (confirmation === "rotate") {
        setAddress(await rotateInboundEmailAddress());
        snackbar.success(t("email_forwarding.rotated"));
      } else {
        await revokeInboundEmailAddress();
        setAddress({ active: false, address: null, createdAt: null });
        snackbar.success(t("email_forwarding.revoked"));
      }
      setConfirmation(null);
    } catch {
      snackbar.error(t("email_forwarding.action_error"));
    } finally {
      setActionPending(false);
    }
  };

  return (
    <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
      <CardContent sx={{ p: { xs: 0, md: 3 } }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
          <ForwardToInboxIcon sx={{ color: "primary.main" }} />
          <Typography variant="h6" component="h2" sx={{ fontWeight: 600 }}>
            {t("email_forwarding.title")}
          </Typography>
        </Box>

        <Typography color="text.secondary" sx={{ mb: 2 }}>
          {t("email_forwarding.description")}
        </Typography>

        {loading && (
          <Box role="status" aria-label={t("email_forwarding.loading")} sx={{ py: 2 }}>
            <CircularProgress size={28} />
          </Box>
        )}

        {!loading && loadFailed && (
          <Alert
            severity="error"
            action={
              <Button color="inherit" size="small" onClick={() => void loadAddress()}>
                {t("email_forwarding.retry")}
              </Button>
            }
          >
            {t("email_forwarding.load_error")}
          </Alert>
        )}

        {!loading && !loadFailed && !address?.active && (
          <Box>
            <Typography sx={{ mb: 2 }}>{t("email_forwarding.inactive")}</Typography>
            <Button
              variant="contained"
              onClick={() => void generateAddress()}
              disabled={actionPending}
            >
              {t("email_forwarding.generate")}
            </Button>
          </Box>
        )}

        {!loading && !loadFailed && address?.active && address.address && (
          <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
            <Box sx={{ display: "flex", gap: 1, alignItems: "flex-start", flexWrap: "wrap" }}>
              <TextField
                label={t("email_forwarding.address_label")}
                value={address.address}
                slotProps={{ htmlInput: { readOnly: true } }}
                sx={{ flex: "1 1 360px" }}
              />
              <Button
                variant="outlined"
                startIcon={<ContentCopyIcon />}
                onClick={() => void copyAddress()}
                sx={{ minHeight: 56 }}
              >
                {t("email_forwarding.copy")}
              </Button>
            </Box>

            <Alert severity="info">{t("email_forwarding.privacy")}</Alert>

            <Box>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                {t("email_forwarding.gmail_title")}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {t("email_forwarding.gmail_instructions")}
              </Typography>
            </Box>

            <Box>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                {t("email_forwarding.outlook_title")}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {t("email_forwarding.outlook_instructions")}
              </Typography>
            </Box>

            <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => setConfirmation("rotate")}
                disabled={actionPending}
              >
                {t("email_forwarding.rotate")}
              </Button>
              <Button
                color="error"
                startIcon={<DeleteOutlineIcon />}
                onClick={() => setConfirmation("revoke")}
                disabled={actionPending}
              >
                {t("email_forwarding.revoke")}
              </Button>
            </Box>
          </Box>
        )}
      </CardContent>

      <Dialog open={confirmation !== null} onClose={() => !actionPending && setConfirmation(null)}>
        <DialogTitle>
          {confirmation === "rotate"
            ? t("email_forwarding.rotate_confirm_title")
            : t("email_forwarding.revoke_confirm_title")}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {confirmation === "rotate"
              ? t("email_forwarding.rotate_confirm_body")
              : t("email_forwarding.revoke_confirm_body")}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmation(null)} disabled={actionPending}>
            {t("common.cancel")}
          </Button>
          <Button
            variant="contained"
            color={confirmation === "revoke" ? "error" : "primary"}
            onClick={() => void confirmLifecycleAction()}
            disabled={actionPending}
          >
            {confirmation === "rotate"
              ? t("email_forwarding.rotate_confirm")
              : t("email_forwarding.revoke_confirm")}
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
}
