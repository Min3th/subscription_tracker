import { useState, useEffect, useMemo } from "react";
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  LinearProgress,
  Stack,
  Avatar,
  IconButton,
  Chip,
  useTheme,
  alpha,
} from "@mui/material";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import AddIcon from "@mui/icons-material/Add";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import SubscriptionForm from "../components/SubscriptionForm";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import AttachMoneyIcon from "@mui/icons-material/AttachMoney";
import StatCard from "../components/StatCard";
import { getSubscriptions } from "../api/subscription";
import { useTranslation } from "react-i18next";

const getNextBillingDate = (startDateStr: string, unit: string, count: number): Date => {
  if (!startDateStr || !unit || !count) return new Date();
  const start = new Date(startDateStr);
  const now = new Date();
  if (start > now) return start;

  const next = new Date(start);
  while (next <= now) {
    if (unit === "day") next.setDate(next.getDate() + count);
    else if (unit === "week") next.setDate(next.getDate() + count * 7);
    else if (unit === "month") next.setMonth(next.getMonth() + count);
    else if (unit === "year") next.setFullYear(next.getFullYear() + count);
    else break;
  }
  return next;
};

const getMonthlyCost = (cost: number, unit: string, count: number): number => {
  if (!cost || !unit || !count) return 0;
  if (unit === "day") return (cost / count) * 30.416;
  if (unit === "week") return (cost / count) * 4.333;
  if (unit === "month") return cost / count;
  if (unit === "year") return cost / (count * 12);
  return 0;
};

const generatePast6MonthsSpending = (subscriptions: any[]) => {
  const monthsData = [];
  const now = new Date();
  
  for (let i = 5; i >= 0; i--) {
    const targetMonth = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const targetMonthEnd = new Date(now.getFullYear(), now.getMonth() - i + 1, 0);
    const monthLabel = targetMonth.toLocaleDateString('en-US', { month: 'short' });
    
    let amount = 0;
    
    subscriptions.forEach(sub => {
      if (sub.type === "one-time") {
        const d = new Date(sub.startDate);
        if (d >= targetMonth && d <= targetMonthEnd) {
          amount += sub.cost;
        }
      } else {
        if (!sub.startDate || !sub.billingIntervalUnit || !sub.billingIntervalCount) return;
        const start = new Date(sub.startDate);
        if (start > targetMonthEnd) return;
        
        let current = new Date(start);
        while (current <= targetMonthEnd) {
          if (current >= targetMonth) {
            amount += sub.cost;
          }
          if (sub.billingIntervalUnit === "day") current.setDate(current.getDate() + sub.billingIntervalCount);
          else if (sub.billingIntervalUnit === "week") current.setDate(current.getDate() + sub.billingIntervalCount * 7);
          else if (sub.billingIntervalUnit === "month") current.setMonth(current.getMonth() + sub.billingIntervalCount);
          else if (sub.billingIntervalUnit === "year") current.setFullYear(current.getFullYear() + sub.billingIntervalCount);
          else break; 
        }
      }
    });
    
    monthsData.push({ month: monthLabel, amount: Number(amount.toFixed(2)) });
  }
  return monthsData;
};

export default function Dashboard() {
  const { t } = useTranslation();
  const theme = useTheme();
  const [subscriptions, setSubscriptions] = useState<any[]>([]);
  const [open, setOpen] = useState(false);
  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const totalMonthly = useMemo(
    () => subscriptions.reduce((sum, sub) => {
      if (sub.type === "one-time") return sum;
      return sum + getMonthlyCost(sub.cost, sub.billingIntervalUnit, sub.billingIntervalCount);
    }, 0),
    [subscriptions],
  );

  const totalYearly = useMemo(
    () => subscriptions.reduce((sum, sub) => {
      if (sub.type === "one-time") return sum;
      return sum + getMonthlyCost(sub.cost, sub.billingIntervalUnit, sub.billingIntervalCount) * 12;
    }, 0),
    [subscriptions],
  );

  const nextBilling = useMemo(() => {
    if (!subscriptions.length) return null;
    const recurringSubs = subscriptions.filter(sub => sub.type !== "one-time");
    if (!recurringSubs.length) return null;
    return recurringSubs.sort(
      (a, b) => getNextBillingDate(a.startDate, a.billingIntervalUnit, a.billingIntervalCount).getTime() - getNextBillingDate(b.startDate, b.billingIntervalUnit, b.billingIntervalCount).getTime(),
    )[0];
  }, [subscriptions]);

  const monthlySpendingData = useMemo(() => generatePast6MonthsSpending(subscriptions), [subscriptions]);

  // Category aggregation
  const categoryData = useMemo(
    () =>
      Object.entries(
        subscriptions.reduce(
          (acc, sub) => {
            let cost = 0;
            if (sub.type !== "one-time") {
              cost = getMonthlyCost(sub.cost, sub.billingIntervalUnit, sub.billingIntervalCount);
            }
            if (cost > 0) {
              acc[sub.category] = (acc[sub.category] || 0) + cost;
            }
            return acc;
          },
          {} as Record<string, number>,
        ),
      ).sort((a, b) => b[1] - a[1]),
    [subscriptions],
  );

  useEffect(() => {
    const fetchSubscriptions = async () => {
      try {
        const res = await getSubscriptions();
        console.log("Fetched subscriptions:", res);
        setSubscriptions(res.data);
      } catch (error: any) {
        console.error("Error fetching subscriptions:", error);
      }
    };
    fetchSubscriptions();
  }, []);

  return (
    <Box
      sx={{
        p: { xs: 2, sm: 3, md: 4 },
        maxWidth: "1400px",
        width: "100%",
        mx: "auto",
        backgroundColor: "transparent",
        minHeight: "100vh",
      }}
      display="flex"
      flexDirection="column"
    >
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: { xs: "flex-start", sm: "center" },
          flexDirection: { xs: "column", sm: "row" },
          gap: 2,
          mb: 4,
        }}
      >
        <Box display="flex" flexDirection="column" gap={0.5}>
          <Typography variant="h4" fontWeight="800" color="text.primary">
            {t("dashboard.title")}
          </Typography>
          <Typography variant="subtitle1" color="text.secondary">
            {t("dashboard.subtitle")}
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          sx={{
            px: 3,
            py: 1.2,
            fontSize: "0.875rem",
            fontWeight: "bold",
            borderRadius: 2,
            textTransform: "none",
            boxShadow: `0 4px 12px ${alpha(theme.palette.primary.main, 0.3)}`,
            "&:hover": {
              boxShadow: `0 6px 16px ${alpha(theme.palette.primary.main, 0.4)}`,
            },
          }}
          onClick={handleOpen}
        >
          {t("dashboard.add_subscription")}
        </Button>
      </Box>

      <Grid container spacing={3} mb={4}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard
            title={t("dashboard.monthly_spent")}
            value={`$${totalMonthly.toFixed(2)}`}
            icon={<AttachMoneyIcon />}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard
            title={t("dashboard.yearly_projected")}
            value={`$${totalYearly.toFixed(2)}`}
            icon={<TrendingUpIcon />}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard
            title={t("dashboard.active_subscriptions")}
            value={subscriptions.length}
            icon={<CreditCardIcon />}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard
            title={t("dashboard.next_billing")}
            value={
              nextBilling
                ? getNextBillingDate(nextBilling.startDate, nextBilling.billingIntervalUnit, nextBilling.billingIntervalCount).toLocaleDateString("en-US", { month: "short", day: "numeric" })
                : "N/A"
            }
            icon={<CalendarTodayIcon />}
          />
        </Grid>
      </Grid>

      <SubscriptionForm handleClose={handleClose} open={open} />

      <Grid container spacing={3} mb={4} alignItems="stretch">
        <Grid size={{ xs: 12, md: 8 }}>
          <Card sx={{ borderRadius: 3, height: "100%", boxShadow: "0 4px 20px rgba(0,0,0,0.03)" }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" mb={3} fontWeight="700">
                {t("dashboard.monthly_overview")}
              </Typography>
              <Box sx={{ height: 320, width: "100%" }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={monthlySpendingData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={theme.palette.divider} />
                    <XAxis
                      dataKey="month"
                      axisLine={false}
                      tickLine={false}
                      tick={{ fontSize: 13, fill: theme.palette.text.secondary }}
                      dy={10}
                    />
                    <YAxis
                      axisLine={false}
                      tickLine={false}
                      tickFormatter={(val) => `$${val}`}
                      tick={{ fontSize: 13, fill: theme.palette.text.secondary }}
                    />
                    <Tooltip
                      cursor={{ fill: alpha(theme.palette.primary.main, 0.05) }}
                      contentStyle={{ borderRadius: 8, border: "none", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}
                    />
                    <Bar
                      dataKey="amount"
                      fill={theme.palette.primary.main || "#7C3AED"}
                      radius={[6, 6, 0, 0]}
                      barSize={36}
                    />
                  </BarChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ borderRadius: 3, height: "100%", boxShadow: "0 4px 20px rgba(0,0,0,0.03)" }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" mb={3} fontWeight="700">
                {t("dashboard.spending_category")}
              </Typography>
              <Stack spacing={3}>
                {categoryData.length > 0 ? (
                  categoryData.map(([category, value]) => {
                    const numValue = value as number;
                    const percent = totalMonthly > 0 ? (numValue / totalMonthly) * 100 : 0;
                    return (
                      <Box key={category}>
                        <Box
                          sx={{
                            display: "flex",
                            justifyContent: "space-between",
                            mb: 1,
                          }}
                        >
                          <Typography variant="body2" fontWeight="600" color="text.secondary">
                            {category}
                          </Typography>
                          <Typography variant="body2" fontWeight="700" color="text.primary">
                            ${numValue.toFixed(2)}
                          </Typography>
                        </Box>
                        <LinearProgress
                          variant="determinate"
                          value={percent}
                          sx={{
                            height: 8,
                            borderRadius: 4,
                            bgcolor: alpha(theme.palette.primary.main, 0.1),
                            "& .MuiLinearProgress-bar": {
                              borderRadius: 4,
                            },
                          }}
                        />
                      </Box>
                    );
                  })
                ) : (
                  <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: "center" }}>
                    {t("dashboard.no_category_data")}
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Typography variant="h5" mb={3} fontWeight="700" color="text.primary">
        {t("dashboard.your_subscriptions")}
      </Typography>
      <Grid container spacing={3}>
        {subscriptions.length > 0 ? (
          subscriptions.map((sub) => (
            <Grid size={{ xs: 12, md: 6, lg: 4 }} key={sub.id}>
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
                  <Box display="flex" gap={2}>
                    <Avatar
                      sx={{
                        width: 52,
                        height: 52,
                        fontSize: "1.2rem",
                        fontWeight: "bold",
                        background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main || "#9333EA"})`,
                        boxShadow: `0 4px 10px ${alpha(theme.palette.primary.main, 0.3)}`,
                      }}
                    >
                      {sub.name[0]?.toUpperCase()}
                    </Avatar>

                    <Box>
                      <Box display="flex" alignItems="center" gap={1} mb={0.5}>
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
                            color: theme.palette.primary.main,
                          }}
                        />
                      </Box>
                      <Typography variant="body2" color="text.secondary" fontWeight="500">
                        <Typography component="span" fontWeight="700" color="text.primary">
                          ${Number(sub.cost).toFixed(2)}
                        </Typography>{" "}
                        {sub.type === "one-time" ? "one-time" : `/ every ${sub.billingIntervalCount > 1 ? sub.billingIntervalCount + " " : ""}${sub.billingIntervalUnit}${sub.billingIntervalCount > 1 ? "s" : ""}`}
                      </Typography>
                      {sub.type !== "one-time" && (
                        <Box display="flex" alignItems="center" gap={0.8} mt={1}>
                          <CalendarTodayIcon sx={{ fontSize: 14, color: theme.palette.text.secondary }} />
                          <Typography variant="caption" color="text.secondary" fontWeight="500">
                            {t("dashboard.next_billing_date")}{" "}
                            {getNextBillingDate(sub.startDate, sub.billingIntervalUnit, sub.billingIntervalCount).toLocaleDateString("en-US", {
                              month: "short",
                              day: "numeric",
                              year: "numeric",
                            })}
                          </Typography>
                        </Box>
                      )}
                    </Box>
                  </Box>

                  <IconButton size="small" sx={{ color: theme.palette.text.secondary }}>
                    <MoreVertIcon />
                  </IconButton>
                </Box>
              </Card>
            </Grid>
          ))
        ) : (
          <Grid size={{ xs: 12 }}>
            <Box
              py={6}
              display="flex"
              flexDirection="column"
              alignItems="center"
              bgcolor="background.paper"
              borderRadius={3}
              border={1}
              borderColor="divider"
              sx={{ borderStyle: "dashed" }}
            >
              <Typography variant="h6" color="text.secondary" mb={1}>
                {t("dashboard.no_subscriptions")}
              </Typography>
              <Typography variant="body2" color="text.disabled" mb={3}>
                {t("dashboard.click_add")}
              </Typography>
              <Button variant="outlined" onClick={handleOpen} startIcon={<AddIcon />}>
                {t("dashboard.add_first")}
              </Button>
            </Box>
          </Grid>
        )}
      </Grid>
    </Box>
  );
}
