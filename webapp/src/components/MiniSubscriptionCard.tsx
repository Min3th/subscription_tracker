import { Card, Box, Typography, Avatar, Chip } from "@mui/material";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import { alpha, useTheme } from "@mui/material/styles";
import type { DetailedSubscription } from "../types/subscription";

export interface MiniSubscriptionCardProps {
  sub: DetailedSubscription;
  t: (key: string) => string;
}

export default function MiniSubscriptionCard({ sub, t }: MiniSubscriptionCardProps) {
  const theme = useTheme();

  return (
    <Card
      sx={{
        borderRadius: 3,
        p: 2.5,
        boxShadow: "0 4px 20px rgba(0,0,0,0.03)",
        width: "100%",
        transition: "transform 0.2s, box-shadow 0.2s",
        "&:hover": {
          transform: "translateY(-4px)",
          boxShadow: "0 8px 24px rgba(0,0,0,0.08)",
        },
      }}
    >
      <Box display="flex" justifyContent="space-between" alignItems="flex-start">
        <Box display="flex" gap={2} alignItems="flex-start" width="100%">
          <Avatar
            sx={{
              width: 52,
              height: 52,
              flexShrink: 0,
              fontSize: "1.2rem",
              fontWeight: "bold",
              background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${
                theme.palette.secondary.main || "#9333EA"
              })`,
              boxShadow: `0 4px 10px ${alpha(theme.palette.primary.main, 0.3)}`,
            }}
          >
            {sub.name[0]?.toUpperCase()}
          </Avatar>

          <Box
            sx={{
              flex: 1,
              minWidth: 0,
              display: "flex",
              flexDirection: "column",
              alignItems: "flex-start",
            }}
          >
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                gap: 1,
                mb: 0.5,
                width: "100%",
                minWidth: 0,
              }}
            >
              <Typography fontWeight="700" fontSize="1.05rem">
                {sub.name}
              </Typography>

              <Chip
                label={sub.category}
                size="small"
                sx={{
                  fontSize: "0.65rem",
                  fontWeight: "bold",
                  height: 22,
                  bgcolor: alpha(theme.palette.primary.main, 0.1),
                  color: theme.palette.purpink,
                }}
              />
            </Box>

            <Typography variant="body2" color="text.secondary" fontWeight="500">
              <Typography component="span" fontWeight="700" color="text.primary">
                ${Number(sub.cost).toFixed(2)}
              </Typography>{" "}
              {sub.type === "one-time"
                ? "one-time"
                : `/ every ${
                    sub.billingIntervalCount > 1 ? sub.billingIntervalCount + " " : ""
                  }${sub.billingIntervalUnit}${sub.billingIntervalCount > 1 ? "s" : ""}`}
            </Typography>

            {sub.type !== "one-time" && (
              <Box display="flex" alignItems="center" gap={0.8} mt={1}>
                <CalendarTodayIcon sx={{ fontSize: 14, color: theme.palette.text.secondary }} />
                <Typography variant="caption" color="text.secondary" fontWeight="500">
                  {t("dashboard.next_billing_date")} {new Date(sub.nextBillingDate).toDateString()}
                </Typography>
              </Box>
            )}
          </Box>
        </Box>
      </Box>
    </Card>
  );
}
