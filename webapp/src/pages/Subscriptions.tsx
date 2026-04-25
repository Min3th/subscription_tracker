import {
  Box,
  TextField,
  InputAdornment,
  Button,
  Stack,
  Typography,
  Menu,
  MenuItem,
  DialogTitle,
  DialogContent,
  Dialog,
  DialogContentText,
  DialogActions,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import AddIcon from "@mui/icons-material/Add";
import SubscriptionCard from "../components/SubscriptionCard";
import FilterAltOutlinedIcon from "@mui/icons-material/FilterAltOutlined";
import SwapVertOutlinedIcon from "@mui/icons-material/SwapVertOutlined";
import ViewListIcon from "@mui/icons-material/ViewList";
import GridViewIcon from "@mui/icons-material/GridView";
import { useEffect, useState } from "react";
import Grid from "@mui/material/Grid";
import { deleteSubscription, getSubscriptions } from "../api/subscription";
import GridSubscriptionCard from "../components/GridSubscriptionCard";
import SubscriptionForm from "../components/SubscriptionForm";
import { ToggleButtonGroup, ToggleButton } from "@mui/material";
import { useTranslation } from "react-i18next";
import type { DetailedSubscription } from "../types/subscription";
import { useDispatch, useSelector } from "react-redux";
import type { RootState } from "../app/store";
import { fetchSubscriptions } from "../app/subscriptionSlice";

export const getNextBillingDate = (startDateStr: string, unit: string, count: number): Date => {
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

export const calculateTotalPaid = (startDateStr: string, unit: string, count: number, cost: number): number => {
  if (!startDateStr || !unit || !count || !cost) return 0;

  const start = new Date(startDateStr);
  const now = new Date();

  if (start > now) return 0;

  let cycles = 0;
  const current = new Date(start);

  while (current <= now) {
    cycles++;

    if (unit === "day") current.setDate(current.getDate() + count);
    else if (unit === "week") current.setDate(current.getDate() + count * 7);
    else if (unit === "month") current.setMonth(current.getMonth() + count);
    else if (unit === "year") current.setFullYear(current.getFullYear() + count);
    else break;
  }

  return cycles * cost;
};

export default function Subscriptions() {
  const { t } = useTranslation();
  const [view, setView] = useState<"grid" | "list">("list");
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState("");
  const [filterCategory, setFilterCategory] = useState("all");
  const [sortBy, setSortBy] = useState("name");
  const [isAddFormOpen, setIsAddFormOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const handleEdit = (id: string) => {
    setEditId(id);
    setIsAddFormOpen(true);
  };
  const subscriptions = useSelector((state: RootState) => state.subscriptions.list);
  const [filterAnchorEl, setFilterAnchorEl] = useState<null | HTMLElement>(null);
  const [sortAnchorEl, setSortAnchorEl] = useState<null | HTMLElement>(null);

  const filteredSubscriptions = subscriptions
    .filter((sub) => {
      const matchesSearch = sub.name.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesCategory = filterCategory === "all" || sub.category === filterCategory;
      return matchesSearch && matchesCategory;
    })
    .sort((a, b) => {
      switch (sortBy) {
        case "name":
          return a.name.localeCompare(b.name);
        case "cost":
          return b.cost - a.cost;
        case "nextBilling":
          return new Date(a.nextBillingDate).getTime() - new Date(b.nextBillingDate).getTime();
        default:
          return 0;
      }
    });

  useEffect(() => {
    dispatch(fetchSubscriptions());
  }, [dispatch]);

  return (
    <Box
      sx={{
        width: "100%",
        display: "flex",
        justifyContent: "center",
        minHeight: "100vh",
        paddingTop: 3,
        paddingBottom: 3,
      }}
    >
      <Box sx={{ width: "100%", maxWidth: 1400 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
          <TextField
            placeholder={t("subscriptions.search", "Search subscriptions...")}
            size="small"
            sx={{ width: 300 }}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />

          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              onClick={(e) => setFilterAnchorEl(e.currentTarget)}
              startIcon={<FilterAltOutlinedIcon fontSize="small" />}
            >
              {filterCategory === "all" ? t("subscriptions.all_categories", "All Categories") : filterCategory}
            </Button>
            <Menu anchorEl={filterAnchorEl} open={Boolean(filterAnchorEl)} onClose={() => setFilterAnchorEl(null)}>
              {["all", ...Array.from(new Set(subscriptions.map((s) => s.category)))].map((cat) => (
                <MenuItem
                  key={cat}
                  onClick={() => {
                    setFilterCategory(cat);
                    setFilterAnchorEl(null);
                  }}
                >
                  {cat === "all" ? t("subscriptions.all_categories", "All Categories") : cat}
                </MenuItem>
              ))}
            </Menu>
            <Button
              variant="outlined"
              onClick={(e) => setSortAnchorEl(e.currentTarget)}
              startIcon={<SwapVertOutlinedIcon fontSize="small" />}
            >
              {t("subscriptions.sort", "Sort")}
            </Button>
            <Menu anchorEl={sortAnchorEl} open={Boolean(sortAnchorEl)} onClose={() => setSortAnchorEl(null)}>
              <MenuItem
                onClick={() => {
                  setSortBy("name");
                  setSortAnchorEl(null);
                }}
              >
                {t("subscriptions.sort_name", "Name")}
              </MenuItem>
              <MenuItem
                onClick={() => {
                  setSortBy("cost");
                  setSortAnchorEl(null);
                }}
              >
                {t("subscriptions.sort_price_desc", "Price (High to Low)")}
              </MenuItem>
              <MenuItem
                onClick={() => {
                  setSortBy("nextBilling");
                  setSortAnchorEl(null);
                }}
              >
                {t("subscriptions.sort_next_billing", "Next Billing Date")}
              </MenuItem>
            </Menu>
            <ToggleButtonGroup
              value={view}
              exclusive
              onChange={(_, newView) => {
                if (newView !== null) setView(newView);
              }}
              size="small"
            >
              <ToggleButton value="list">
                <ViewListIcon fontSize="small" />
              </ToggleButton>

              <ToggleButton value="grid">
                <GridViewIcon fontSize="small" />
              </ToggleButton>
            </ToggleButtonGroup>
          </Stack>
        </Stack>
        <Grid container spacing={2}>
          {filteredSubscriptions.length > 0 ? (
            filteredSubscriptions.map((sub) => (
              <Grid
                key={sub.id}
                size={{
                  xs: 12,
                  sm: view === "grid" ? 6 : 12,
                  md: view === "grid" ? 4 : 12,
                  lg: view === "grid" ? 3 : 12,
                }}
              >
                {view === "list" ? (
                  <SubscriptionCard subscription={sub} onEdit={handleEdit} />
                ) : (
                  <GridSubscriptionCard subscription={sub} onEdit={handleEdit} />
                )}
              </Grid>
            ))
          ) : (
            <Grid size={{ xs: 12 }}>
              <Typography textAlign="center" mt={4}>
                {t("subscriptions.no_found", "No subscriptions found")}
              </Typography>
            </Grid>
          )}
        </Grid>
      </Box>
      <SubscriptionForm
        open={isAddFormOpen}
        handleClose={() => {
          setIsAddFormOpen(false);
          setEditId(null);
        }}
        editId={editId}
      />
    </Box>
  );
}
