import { useState } from "react";
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
} from "@mui/material";
import AttachMoneyIcon from "@mui/icons-material/AttachMoney";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import AddIcon from "@mui/icons-material/Add";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import SideDrawer from "../components/SideDrawer";
import SubscriptionForm from "../components/SubscriptionForm";
import { CreditCard, DollarSign, TrendingUp } from "lucide-react";
import StatCard from "../components/StatCard";
import { getSubscriptions } from "../api/subscription";
import { useEffect } from "react";

const mockSubscriptions = [
  {
    id: "1",
    name: "Netflix",
    cost: 15.99,
    billingCycle: "monthly",
    nextBillingDate: "Apr 15, 2026",
    category: "Entertainment",
    status: "active",
  },
  {
    id: "2",
    name: "Spotify",
    cost: 10.99,
    billingCycle: "monthly",
    nextBillingDate: "Apr 12, 2026",
    category: "Music",
    status: "active",
  },
  {
    id: "3",
    name: "Adobe CC",
    cost: 54.99,
    billingCycle: "monthly",
    nextBillingDate: "Apr 20, 2026",
    category: "Productivity",
    status: "active",
  },
];

const monthlySpendingData = [
  { month: "Oct", amount: 98 },
  { month: "Nov", amount: 102 },
  { month: "Dec", amount: 150 },
  { month: "Jan", amount: 105 },
  { month: "Feb", amount: 102 },
  { month: "Mar", amount: 102 },
];

// ---------------- COMPONENT ----------------

export default function Dashboard() {
  const [subscriptions, setSubscriptions] = useState<any[]>([]);
  const [open, setOpen] = useState(false);

  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const totalMonthly = subscriptions.reduce(
    (sum, sub) => sum + (sub.billingCycle === "monthly" ? sub.cost : sub.cost / 12),
    0,
  );

  const totalYearly = subscriptions.reduce(
    (sum, sub) => sum + (sub.billingCycle === "yearly" ? sub.cost : sub.cost * 12),
    0,
  );

  const nextBilling = subscriptions.sort(
    (a, b) => new Date(a.nextBillingDate).getTime() - new Date(b.nextBillingDate).getTime(),
  )[0];

  // Category aggregation
  const categoryData = Object.entries(
    subscriptions.reduce(
      (acc, sub) => {
        const cost = sub.billingCycle === "monthly" ? sub.cost : sub.cost / 12;
        acc[sub.category] = (acc[sub.category] || 0) + cost;
        return acc;
      },
      {} as Record<string, number>,
    ),
  );

  useEffect(() => {
    const fetchSubscriptions = async () => {
      try {
        const res = await getSubscriptions();
        console.log("Fetched subscriptions:", res.data);
        setSubscriptions(res.data);
      } catch (error) {
        console.error("Error fetching subscriptions:", error);
      }
    };
    fetchSubscriptions();
  }, []);

  return (
    <Box
      sx={{
        p: 3,
        width: "100%",
        mx: "auto", // centers everything
      }}
    >
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          mb: 3,
        }}
      >
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Subscription Tracker
          </Typography>
          <Typography color="text.secondary">Manage all your subscriptions</Typography>
        </Box>

        <Button
          variant="contained"
          startIcon={<AddIcon />}
          sx={{
            px: 1,
            py: 1,
            fontSize: "0.7rem",
            height: "32px",
            textTransform: "none",
            fontFamily: "Inter, sans-serif",
          }}
          onClick={handleOpen}
        >
          Add Subscription
        </Button>
      </Box>

      {/* STATS */}
      <Grid container spacing={2} mb={3}>
        <Grid item xs={12} md={3}>
          <StatCard title="Monthly" value={`$${totalMonthly.toFixed(2)}`} icon={<DollarSign />} />
        </Grid>

        <Grid item xs={12} md={3}>
          <StatCard title="Yearly" value={`$${totalYearly.toFixed(2)}`} icon={<TrendingUp />} />
        </Grid>

        <Grid item xs={12} md={3}>
          <StatCard title="Subscriptions" value={subscriptions.length} icon={<CreditCard />} />
        </Grid>

        <Grid item xs={12} md={3}>
          <StatCard title="Next Billing" value={nextBilling?.nextBillingDate || "N/A"} icon={<CalendarTodayIcon />} />
        </Grid>
      </Grid>
      <SubscriptionForm handleClose={handleClose} open={open} />
      {/* CHART + CATEGORY */}
      <Grid container spacing={2} mb={3}>
        {/* Chart */}
        <Grid item xs={12} md={8}>
          <Card sx={{ borderRadius: 3 }}>
            <CardContent>
              <Typography variant="h6">Monthly Spending</Typography>

              <Box sx={{ height: 300, width: "600px" }}>
                <ResponsiveContainer>
                  <BarChart data={monthlySpendingData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="month" />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="amount" fill="#7C3AED" radius={[8, 8, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Category */}
        <Grid item xs={12} md={4}>
          <Card sx={{ borderRadius: 3 }}>
            <CardContent sx={{ width: "300px" }}>
              <Typography variant="h6" mb={2}>
                Categories
              </Typography>

              <Stack spacing={2}>
                {categoryData.map(([category, value]) => {
                  const percent = (value / totalMonthly) * 100;

                  return (
                    <Box key={category}>
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                        }}
                      >
                        <Typography>{category}</Typography>
                        <Typography>${value.toFixed(2)}</Typography>
                      </Box>

                      <LinearProgress
                        variant="determinate"
                        value={percent}
                        sx={{
                          height: 8,
                          borderRadius: 5,
                          mt: 0.5,
                        }}
                      />
                    </Box>
                  );
                })}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* SUBSCRIPTIONS */}
      <Typography variant="h5" mb={2}>
        Your Subscriptions
      </Typography>

      <Grid container spacing={2}>
        {subscriptions.map((sub) => (
          <Grid item xs={12} md={6} key={sub.id}>
            <Card
              sx={{
                borderRadius: 3,
                p: 2,
                boxShadow: "0 4px 20px rgba(0,0,0,0.08)",
                width: "500px",
              }}
            >
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                {/* LEFT SIDE */}
                <Box display="flex" gap={2}>
                  {/* ICON */}
                  <Avatar
                    sx={{
                      width: 48,
                      height: 48,
                      fontWeight: "bold",
                      background: "linear-gradient(135deg, #6366F1, #9333EA)",
                    }}
                  >
                    {sub.name[0]}
                  </Avatar>

                  {/* TEXT */}
                  <Box>
                    <Box display="flex" alignItems="center" gap={1}>
                      <Typography fontWeight="600">{sub.name}</Typography>

                      <Chip
                        label={sub.category}
                        size="small"
                        sx={{
                          fontSize: "0.7rem",
                          height: 22,
                        }}
                      />
                    </Box>

                    <Typography variant="body2" mt={0.5}>
                      $ {sub.cost} / {sub.billingCycle}
                    </Typography>

                    <Box display="flex" alignItems="center" gap={1} mt={0.5}>
                      <CalendarTodayIcon sx={{ fontSize: 14 }} />
                      <Typography variant="caption">Next billing: {sub.nextBillingDate}</Typography>
                    </Box>
                  </Box>
                </Box>

                {/* RIGHT SIDE MENU */}
                <IconButton size="small">
                  <MoreVertIcon />
                </IconButton>
              </Box>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

// ---------------- STAT CARD ----------------
