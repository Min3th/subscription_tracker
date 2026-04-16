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

const monthlySpendingData = [
  { month: "Oct", amount: 98 },
  { month: "Nov", amount: 102 },
  { month: "Dec", amount: 150 },
  { month: "Jan", amount: 105 },
  { month: "Feb", amount: 102 },
  { month: "Mar", amount: 102 },
];

export default function Dashboard() {
  const { t } = useTranslation();
  const theme = useTheme();
  const [subscriptions, setSubscriptions] = useState<any[]>([]);
  const [open, setOpen] = useState(false);
  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const totalMonthly = useMemo(() => subscriptions.reduce(
    (sum, sub) => sum + (sub.billingCycle === "monthly" ? sub.cost : sub.cost / 12),
    0,
  ), [subscriptions]);

  const totalYearly = useMemo(() => subscriptions.reduce(
    (sum, sub) => sum + (sub.billingCycle === "yearly" ? sub.cost : sub.cost * 12),
    0,
  ), [subscriptions]);

  const nextBilling = useMemo(() => {
    if (!subscriptions.length) return null;
    return [...subscriptions].sort(
      (a, b) => new Date(a.nextBillingDate).getTime() - new Date(b.nextBillingDate).getTime(),
    )[0];
  }, [subscriptions]);

  // Category aggregation
  const categoryData = useMemo(() => Object.entries(
    subscriptions.reduce(
      (acc, sub) => {
        const cost = sub.billingCycle === "monthly" ? sub.cost : sub.cost / 12;
        acc[sub.category] = (acc[sub.category] || 0) + cost;
        return acc;
      },
      {} as Record<string, number>,
    ),
  ).sort((a, b) => b[1] - a[1]), [subscriptions]);

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
            {t('dashboard.title')}
          </Typography>
          <Typography variant="subtitle1" color="text.secondary">
            {t('dashboard.subtitle')}
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
            }
          }}
          onClick={handleOpen}
        >
          {t('dashboard.add_subscription')}
        </Button>
      </Box>

      {/* STATS */}
      <Grid container spacing={3} mb={4}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard title={t('dashboard.monthly_spent')} value={`$${totalMonthly.toFixed(2)}`} icon={<AttachMoneyIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard title={t('dashboard.yearly_projected')} value={`$${totalYearly.toFixed(2)}`} icon={<TrendingUpIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard title={t('dashboard.active_subscriptions')} value={subscriptions.length} icon={<CreditCardIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard 
            title={t('dashboard.next_billing')} 
            value={nextBilling ? new Date(nextBilling.nextBillingDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : "N/A"} 
            icon={<CalendarTodayIcon />} 
          />
        </Grid>
      </Grid>

      <SubscriptionForm handleClose={handleClose} open={open} />

      {/* CHART + CATEGORY */}
      <Grid container spacing={3} mb={4} alignItems="stretch">
        {/* Chart */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card sx={{ borderRadius: 3, height: "100%", boxShadow: "0 4px 20px rgba(0,0,0,0.03)" }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" mb={3} fontWeight="700">{t('dashboard.monthly_overview')}</Typography>
              <Box sx={{ height: 320, width: "100%" }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={monthlySpendingData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={theme.palette.divider} />
                    <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize: 13, fill: theme.palette.text.secondary }} dy={10} />
                    <YAxis axisLine={false} tickLine={false} tickFormatter={(val) => `$${val}`} tick={{ fontSize: 13, fill: theme.palette.text.secondary }} />
                    <Tooltip 
                      cursor={{ fill: alpha(theme.palette.primary.main, 0.05) }}
                      contentStyle={{ borderRadius: 8, border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                    />
                    <Bar dataKey="amount" fill={theme.palette.primary.main || "#7C3AED"} radius={[6, 6, 0, 0]} barSize={36} />
                  </BarChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Category */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ borderRadius: 3, height: "100%", boxShadow: "0 4px 20px rgba(0,0,0,0.03)" }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" mb={3} fontWeight="700">
                {t('dashboard.spending_category')}
              </Typography>
              <Stack spacing={3}>
                {categoryData.length > 0 ? categoryData.map(([category, value]) => {
                  const numValue = value as number;
                  const percent = totalMonthly > 0 ? (numValue / totalMonthly) * 100 : 0;
                  return (
                    <Box key={category}>
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          mb: 1
                        }}
                      >
                        <Typography variant="body2" fontWeight="600" color="text.secondary">{category}</Typography>
                        <Typography variant="body2" fontWeight="700" color="text.primary">${numValue.toFixed(2)}</Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={percent}
                        sx={{
                          height: 8,
                          borderRadius: 4,
                          bgcolor: alpha(theme.palette.primary.main, 0.1),
                          '& .MuiLinearProgress-bar': {
                            borderRadius: 4,
                          }
                        }}
                      />
                    </Box>
                  );
                }) : (
                  <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>{t('dashboard.no_category_data')}</Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* SUBSCRIPTIONS */}
      <Typography variant="h5" mb={3} fontWeight="700" color="text.primary">
        {t('dashboard.your_subscriptions')}
      </Typography>
      <Grid container spacing={3}>
        {subscriptions.length > 0 ? subscriptions.map((sub) => (
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
                }
              }}
            >
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                {/* LEFT SIDE */}
                <Box display="flex" gap={2}>
                  {/* ICON */}
                  <Avatar
                    sx={{
                      width: 52,
                      height: 52,
                      fontSize: "1.2rem",
                      fontWeight: "bold",
                      background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main || '#9333EA'})`,
                      boxShadow: `0 4px 10px ${alpha(theme.palette.primary.main, 0.3)}`
                    }}
                  >
                    {sub.name[0]?.toUpperCase()}
                  </Avatar>
                  {/* TEXT */}
                  <Box>
                    <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                      <Typography fontWeight="700" fontSize="1.05rem">{sub.name}</Typography>
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
                      </Typography> / {sub.billingCycle}
                    </Typography>
                    <Box display="flex" alignItems="center" gap={0.8} mt={1}>
                      <CalendarTodayIcon sx={{ fontSize: 14, color: theme.palette.text.secondary }} />
                      <Typography variant="caption" color="text.secondary" fontWeight="500">
                        {t('dashboard.next_billing_date')} {new Date(sub.nextBillingDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                      </Typography>
                    </Box>
                  </Box>
                </Box>
                {/* RIGHT SIDE MENU */}
                <IconButton size="small" sx={{ color: theme.palette.text.secondary }}>
                  <MoreVertIcon />
                </IconButton>
              </Box>
            </Card>
          </Grid>
        )) : (
          <Grid size={{ xs: 12 }}>
            <Box py={6} display="flex" flexDirection="column" alignItems="center" bgcolor="background.paper" borderRadius={3} border={1} borderColor="divider" sx={{ borderStyle: 'dashed' }}>
              <Typography variant="h6" color="text.secondary" mb={1}>{t('dashboard.no_subscriptions')}</Typography>
              <Typography variant="body2" color="text.disabled" mb={3}>{t('dashboard.click_add')}</Typography>
              <Button variant="outlined" onClick={handleOpen} startIcon={<AddIcon />}>{t('dashboard.add_first')}</Button>
            </Box>
          </Grid>
        )}
      </Grid>
    </Box>
  );
}
