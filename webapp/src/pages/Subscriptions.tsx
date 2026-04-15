import { Box, TextField, InputAdornment, Button, Stack, Typography } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import SubscriptionCard, { type DetailedSubscription } from "../components/SubscriptionCard";
import FilterAltOutlinedIcon from "@mui/icons-material/FilterAltOutlined";
import SwapVertOutlinedIcon from "@mui/icons-material/SwapVertOutlined";
import ViewListIcon from "@mui/icons-material/ViewList";
import GridViewIcon from "@mui/icons-material/GridView";
import { useEffect, useState } from "react";
import Grid from "@mui/material/Grid";
import { getSubscriptions } from "../api/subscription";
import GridSubscriptionCard from "../components/GridSubscriptionCard";

export default function Subscriptions() {
  const [view, setView] = useState<"grid" | "list">("list");
  console.log("Current view:", view);
  const [subscriptions, setSubscriptions] = useState<DetailedSubscription[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await getSubscriptions();
        console.log("API response:", res.data);

        const mapped = res.data.map(
          (item: any): DetailedSubscription => ({
            id: item.id.toString(),
            name: item.name,
            cost: item.cost,

            // 🔥 mapping DB → UI
            billingCycle: item.duration === "yearly" ? "yearly" : "monthly",
            nextBillingDate: "2026-05-01", // temporary
            category: item.category || "General",
            status: "active",

            // 🔥 missing fields (hardcoded for now)
            paymentMethod: "Visa **** 4242",
            startDate: "2024-01-01",
            description: `${item.name} subscription`,
            website: "https://example.com",
            autoRenew: item.type === "recurring",
            totalPaid: item.cost * 5, // fake calc
          }),
        );

        setSubscriptions(mapped);
      } catch (err) {
        console.error("Error fetching subscriptions:", err);
      }
    };

    fetchData();
  }, []);
  return (
    <Box sx={{ width: "100%", display: "flex", justifyContent: "center" }}>
      <Box sx={{ width: "100%", maxWidth: 1400 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
          <TextField
            placeholder="Search subscriptions..."
            size="small"
            sx={{ width: 300 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />

          <Stack direction="row" spacing={1}>
            <Button variant="outlined" sx={{ textTransform: "none", color: "black", borderColor: "black" }}>
              <FilterAltOutlinedIcon fontSize="small" />
              All Categories
            </Button>
            <Button variant="outlined" sx={{ textTransform: "none", color: "black", borderColor: "black" }}>
              <SwapVertOutlinedIcon fontSize="small" />
              Sort
            </Button>
            <Stack direction="row" spacing={1}>
              <Button onClick={() => setView("list")}>
                <ViewListIcon />
              </Button>
              <Button onClick={() => setView("grid")}>
                <GridViewIcon />
              </Button>
            </Stack>
          </Stack>
        </Stack>

        <Grid container spacing={2}>
          {subscriptions.map((sub) => (
            <Grid
              key={sub.id}
              size={{
                xs: 12,
                sm: view === "grid" ? 6 : 12,
                md: view === "grid" ? 4 : 12,
                lg: view === "grid" ? 3 : 12,
              }}
            >
              {view === "list" ? <SubscriptionCard subscription={sub} /> : <GridSubscriptionCard subscription={sub} />}
            </Grid>
          ))}
        </Grid>
      </Box>
    </Box>
  );
}
