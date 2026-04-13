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

export default function Subscriptions() {
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
        mx: "auto",
      }}
    >
      <Typography variant="h4" gutterBottom>
        Your Subscriptions
      </Typography>
    </Box>
  );
}
