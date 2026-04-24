import { Card, CardContent, Box, Typography, Chip, IconButton, Menu, MenuItem } from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import { useState } from "react";

export default function GridSubscriptionCard({ subscription, onEdit }: any) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

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
    <Card
      sx={{ height: "100%", display: "flex", flexDirection: "column", "&:hover": { boxShadow: 6 }, transition: "0.3s" }}
    >
      <CardContent sx={{ flexGrow: 1, display: "flex", flexDirection: "column" }}>
        <Box display="flex" flexDirection="column" gap={2} flexGrow={1}>
          <Box display="flex" justifyContent="space-between">
            <Box
              sx={{
                width: 56,
                height: 56,
                borderRadius: 2,
                background: "linear-gradient(135deg, #3b82f6, #8b5cf6)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#fff",
                fontWeight: "bold",
                fontSize: 20,
              }}
            >
              {subscription.name.charAt(0)}
            </Box>

            <Box>
              <IconButton size="small" onClick={handleOpen}>
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
              </Menu>
            </Box>
          </Box>

          <Box display="flex" flexDirection="column" alignItems="start">
            <Typography fontWeight={600}>{subscription.name}</Typography>

            <Typography variant="body2" color="text.secondary">
              {subscription.description}
            </Typography>

            <Box display="flex" gap={1} mt={1}>
              <Chip
                label={subscription.status}
                size="small"
                sx={{ backgroundColor: statusStyle.bg, color: statusStyle.color }}
              />
              <Chip
                label={subscription.category}
                size="small"
                sx={{ backgroundColor: categoryStyle.bg, color: categoryStyle.color }}
              />
            </Box>
          </Box>

          <Box>
            <Typography fontSize={22} fontWeight="bold">
              ${subscription.cost.toFixed(2)}
              <Typography component="span" variant="body2">
                /{subscription.billingCycle === "monthly" ? "mo" : "yr"}
              </Typography>
            </Typography>
          </Box>

          <Box display="flex" flexDirection="column" gap={0.5}>
            <Box display="flex" alignItems="center" gap={1}>
              <CalendarTodayIcon sx={{ fontSize: 14 }} />
              <Typography variant="caption">{new Date(subscription.nextBillingDate).toLocaleDateString()}</Typography>
            </Box>

            <Box display="flex" alignItems="center" gap={1}>
              <CreditCardIcon sx={{ fontSize: 14 }} />
              <Typography variant="caption">{subscription.paymentMethod}</Typography>
            </Box>
          </Box>

          <Box display="flex" justifyContent="space-between" pt={1} borderTop={1} borderColor="divider" mt="auto">
            <Typography variant="caption">Total Paid</Typography>
            <Typography variant="caption" fontWeight={500}>
              ${subscription.totalPaid.toFixed(2)}
            </Typography>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}
