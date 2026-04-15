import { Card, CardContent, Box, Typography, Chip, IconButton } from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import CreditCardIcon from "@mui/icons-material/CreditCard";

export default function GridSubscriptionCard({ subscription }: any) {
  return (
    <Card sx={{ "&:hover": { boxShadow: 6 }, transition: "0.3s" }}>
      <CardContent>
        <Box display="flex" flexDirection="column" gap={2}>
          {/* TOP */}
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

            <IconButton size="small">
              <MoreVertIcon />
            </IconButton>
          </Box>

          {/* TITLE */}
          <Box>
            <Typography fontWeight={600}>{subscription.name}</Typography>

            <Typography variant="body2" color="text.secondary">
              {subscription.description}
            </Typography>

            <Box display="flex" gap={1} mt={1}>
              <Chip label={subscription.status} size="small" />
              <Chip label={subscription.category} size="small" />
            </Box>
          </Box>

          {/* PRICE */}
          <Box>
            <Typography fontSize={22} fontWeight="bold">
              ${subscription.cost.toFixed(2)}
              <Typography component="span" variant="body2">
                /{subscription.billingCycle === "monthly" ? "mo" : "yr"}
              </Typography>
            </Typography>
          </Box>

          {/* INFO */}
          <Box display="flex" flexDirection="column" gap={0.5}>
            <Box display="flex" alignItems="center" gap={1}>
              <CalendarTodayIcon sx={{ fontSize: 14 }} />
              <Typography variant="caption">{subscription.nextBillingDate}</Typography>
            </Box>

            <Box display="flex" alignItems="center" gap={1}>
              <CreditCardIcon sx={{ fontSize: 14 }} />
              <Typography variant="caption">{subscription.paymentMethod}</Typography>
            </Box>
          </Box>

          {/* FOOTER */}
          <Box display="flex" justifyContent="space-between" pt={1} borderTop="1px solid #eee">
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
