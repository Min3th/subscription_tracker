import {
  Card,
  CardContent,
  Box,
  Typography,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Divider,
  Link,
  DialogTitle,
  DialogContent,
  Dialog,
  DialogContentText,
  DialogActions,
  Button,
} from "@mui/material";
import { useState } from "react";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import AttachMoneyIcon from "@mui/icons-material/AttachMoney";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import LanguageIcon from "@mui/icons-material/Language";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import { Switch } from "@mui/material";
import { useTheme } from "@mui/material";
import type { DetailedSubscription } from "../types/subscription";
import { formatMoney } from "../utils/money";
import { t } from "i18next";
import { useSnackbar } from "../utils/Snackbar";
import { useDispatch } from "react-redux";
import type { AppDispatch } from "../app/store";
import { deleteSubscriptionThunk } from "../app/subscriptionSlice";

interface Props {
  subscription: DetailedSubscription;
  onEdit?: (id: number) => void;
  onCancel?: (id: number) => void;
  onPause?: (id: number) => void;
}

export default function SubscriptionCard({ subscription, onEdit, onCancel, onPause }: Props) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);
  const theme = useTheme();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const handleOpen = (e: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(e.currentTarget);
  };
  const snackbar = useSnackbar();
  const handleClose = () => setAnchorEl(null);
  const dispatch = useDispatch<AppDispatch>();
  const getStatusColor = (status: string) => {
    switch (status) {
      case "active":
        return { bg: "#e8f5e9", color: "#2e7d32" };
      case "cancelled":
        return { bg: "#ffebee", color: "#c62828" };
      case "paused":
        return { bg: "#fff8e1", color: "#f9a825" };
      default:
        return { bg: "#eee", color: "#555" };
    }
  };

  const getCategoryColor = (category: string) => {
    const map: Record<string, { bg: string; color: string }> = {
      Entertainment: { bg: "#f3e5f5", color: "#7b1fa2" },
      Productivity: { bg: "#e3f2fd", color: "#1565c0" },
      Music: { bg: "#fce4ec", color: "#c2185b" },
    };
    return map[category] || { bg: "#eee", color: "#555" };
  };

  const statusStyle = getStatusColor(subscription.status);
  const categoryStyle = getCategoryColor(subscription.category);

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
  };

  const handleDeleteConfirm = async (id: number) => {
    try {
      dispatch(deleteSubscriptionThunk(id));
      snackbar.success("Subscription deleted successfully.");
    } catch (err) {
      snackbar.error("Failed to delete subscription.");
      console.error("Error deleting subscription:", err);
    }
    setDeleteDialogOpen(false);
  };

  return (
    <Card sx={{ minWidth: 0, overflow: "hidden", "&:hover": { boxShadow: 6 }, transition: "0.3s" }}>
      <CardContent>
        <Box display="flex" flexDirection={{ xs: "column", lg: "row" }} gap={3}>
          <Box display="flex" gap={{ xs: 1.5, sm: 2 }} flex={1} minWidth={0}>
            <Box
              sx={{
                width: { xs: 52, sm: 64 },
                height: { xs: 52, sm: 64 },
                flexShrink: 0,
                borderRadius: 2,
                background: "linear-gradient(135deg, #3b82f6, #8b5cf6)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#fff",
                fontWeight: "bold",
                fontSize: 24,
              }}
            >
              {subscription.name.charAt(0)}
            </Box>

            <Box flex={1} minWidth={0} display="flex" flexDirection="column">
              <Box flex={1} display="flex" flexDirection="column" sx={{ alignItems: "start" }}>
                <Box display="flex" gap={1} alignItems="center" flexWrap={{ xs: "wrap", sm: "nowrap" }} minWidth={0}>
                  <Typography variant="h6" sx={{ overflowWrap: "anywhere" }}>
                    {subscription.name}
                  </Typography>

                  <Chip
                    label={subscription.status}
                    size="small"
                    sx={{
                      backgroundColor: statusStyle.bg,
                      color: statusStyle.color,
                    }}
                  />

                  <Chip
                    label={subscription.category}
                    size="small"
                    sx={{
                      backgroundColor: categoryStyle.bg,
                      color: categoryStyle.color,
                    }}
                  />
                </Box>

                <Box>
                  <Typography variant="body2" color="text.secondary" mt={0.5} mb={2}>
                    {subscription.description}
                  </Typography>
                </Box>
              </Box>

              <Box display="grid" minWidth={0} gridTemplateColumns={{ xs: "minmax(0, 1fr)", md: "1fr 1fr" }} gap={1}>
                <Box display="flex" alignItems="center" gap={1}>
                  <AttachMoneyIcon fontSize="small" />
                  <Typography fontWeight="bold">{formatMoney(subscription.cost, subscription.currency)}</Typography>
                  <Typography variant="body2">/ {subscription.billingIntervalUnit}</Typography>
                </Box>

                <Box display="flex" alignItems="center" gap={1}>
                  <CalendarTodayIcon fontSize="small" />
                  <Typography variant="body2">
                    {subscription.nextBillingDate
                      ? `Next: ${subscription.nextBillingDate.toLocaleDateString()}`
                      : "One-time purchase"}
                  </Typography>
                </Box>

                <Box display="flex" alignItems="center" gap={1}>
                  <CreditCardIcon fontSize="small" />
                  <Typography variant="body2">{subscription.paymentMethod}</Typography>
                </Box>

                <Box display="flex" alignItems="center" gap={1}>
                  <AccessTimeIcon fontSize="small" />
                  <Typography variant="body2">Started: {subscription.startDate}</Typography>
                </Box>

                <Box display="flex" alignItems="center" gap={1}>
                  <TrendingUpIcon fontSize="small" />
                  <Typography variant="body2">Total Paid: {formatMoney(subscription.totalPaid, subscription.currency)}</Typography>
                </Box>

                <Box display="flex" alignItems="center" gap={1}>
                  <LanguageIcon fontSize="small" />
                  <Link
                    href={subscription.website}
                    target="_blank"
                    underline="hover"
                    sx={{
                      color: theme.palette.purpink,
                      minWidth: 0,
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {subscription.website}
                  </Link>
                </Box>
              </Box>
            </Box>
          </Box>
          <Box
            display="flex"
            flexDirection={{ xs: "row", md: "column" }}
            justifyContent={{ xs: "space-between", md: "flex-start" }}
            alignItems="center"
            gap={2}
          >
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="body2">Auto-renew</Typography>
              <Switch checked={subscription.autoRenew} onChange={() => {}} size="small" />
            </Box>

            <IconButton
              aria-label={`Open actions for ${subscription.name}`}
              onClick={handleOpen}
              sx={{ minWidth: { xs: 44, md: "auto" }, minHeight: { xs: 44, md: "auto" } }}
            >
              <MoreVertIcon />
            </IconButton>

            <Menu anchorEl={anchorEl} open={open} onClose={handleClose}>
              <MenuItem
                onClick={() => {
                  onEdit?.(subscription.id);
                  handleClose();
                }}
              >
                Edit
              </MenuItem>

              {subscription.status === "active" && (
                <MenuItem
                  onClick={() => {
                    onPause?.(subscription.id);
                    handleClose();
                  }}
                >
                  Pause
                </MenuItem>
              )}
              <MenuItem
                onClick={() => {
                  setDeleteDialogOpen(true);
                }}
              >
                Delete
              </MenuItem>

              <Divider />

              <MenuItem
                onClick={() => {
                  onCancel?.(subscription.id);
                  handleClose();
                }}
                sx={{ color: "error.main" }}
              >
                Cancel
              </MenuItem>
            </Menu>
          </Box>
        </Box>
      </CardContent>
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title" sx={{ color: "text.primary" }}>
          {t("subscriptions.delete_confirm_title", "Confirm Delete")}
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description" sx={{ color: "text.secondary" }}>
            {t("subscriptions.delete_confirm_message", "Are you sure you want to delete this subscription?")}
          </DialogContentText>
        </DialogContent>
        <DialogActions
          sx={{
            flexDirection: { xs: "column", sm: "row" },
            alignItems: "stretch",
            gap: { xs: 1, sm: 0 },
            "& .MuiButton-root": {
              width: { xs: "100%", sm: "auto" },
              minHeight: { xs: 44, sm: "auto" },
              m: { xs: "0 !important", sm: undefined },
            },
          }}
        >
          <Button onClick={handleDeleteCancel} color="inherit">
            {t("common.cancel", "Cancel")}
          </Button>
          <Button onClick={() => handleDeleteConfirm(subscription.id)} autoFocus color="error">
            {t("subscriptions.delete_confirm_button", "Delete")}
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
}
