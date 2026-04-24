import { Card, CardContent, Box, Typography, Chip, IconButton, Menu, MenuItem, Divider, Link } from "@mui/material";
import { useState } from "react";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import AttachMoneyIcon from "@mui/icons-material/AttachMoney";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import LanguageIcon from "@mui/icons-material/Language";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import { Switch, FormControlLabel } from "@mui/material";
import { useTheme } from "@mui/material";

export interface DetailedSubscription {
  id: string;
  name: string;
  cost: number;
  billingCycle: "monthly" | "yearly";
  nextBillingDate: string;
  category: string;
  status: "active" | "cancelled" | "paused";
  paymentMethod: string;
  startDate: string;
  description: string;
  website: string;
  autoRenew: boolean;
  totalPaid: number;
}

interface Props {
  subscription: DetailedSubscription;
  onEdit?: (id: string) => void;
  onCancel?: (id: string) => void;
  onPause?: (id: string) => void;
}

export default function SubscriptionCard({ subscription, onEdit, onCancel, onPause }: Props) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);
  const theme = useTheme();
  const handleOpen = (e: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(e.currentTarget);
  };

  const handleClose = () => setAnchorEl(null);

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

  return (
    <Card sx={{ "&:hover": { boxShadow: 6 }, transition: "0.3s" }}>
      <CardContent>
        <Box display="flex" flexDirection={{ xs: "column", lg: "row" }} gap={3}>
          <Box display="flex" gap={2} flex={1}>
            <Box
              sx={{
                width: 64,
                height: 64,
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

            <Box flex={1} display="flex" flexDirection="column">
              <Box flex={1} display="flex" flexDirection="column" sx={{ alignItems: "start" }}>
                <Box display="flex" gap={1} alignItems="center">
                  <Typography variant="h6">{subscription.name}</Typography>

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

              <Box display="grid" gridTemplateColumns={{ xs: "1fr", md: "1fr 1fr" }} gap={1}>
                <Box display="flex" alignItems="center" gap={1}>
                  <AttachMoneyIcon fontSize="small" />
                  <Typography fontWeight="bold">${subscription.cost.toFixed(2)}</Typography>
                  <Typography variant="body2">
                    / {subscription.billingCycle === "monthly" ? "month" : "year"}
                  </Typography>
                </Box>

                <Box display="flex" alignItems="center" gap={1}>
                  <CalendarTodayIcon fontSize="small" />
                  <Typography variant="body2">Next: {subscription.nextBillingDate}</Typography>
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
                  <Typography variant="body2">Total Paid: ${subscription.totalPaid.toFixed(2)}</Typography>
                </Box>

                <Box display="flex" alignItems="center" gap={1}>
                  <LanguageIcon fontSize="small" />
                  <Link
                    href={subscription.website}
                    target="_blank"
                    underline="hover"
                    sx={{ color: theme.palette.purpink }}
                  >
                    {subscription.website}
                  </Link>
                </Box>
              </Box>
            </Box>
          </Box>
          <Box display="flex" flexDirection="column" alignItems="flex-end" gap={2}>
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="body2">Auto-renew</Typography>
              <Switch checked={subscription.autoRenew} onChange={(e) => {}} size="small" />
            </Box>

            <IconButton onClick={handleOpen}>
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
    </Card>
  );
}
