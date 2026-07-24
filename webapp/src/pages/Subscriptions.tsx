import { Box, TextField, InputAdornment, Button, Stack, Typography, Menu, MenuItem } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import SubscriptionCard from "../components/SubscriptionCard";
import FilterAltOutlinedIcon from "@mui/icons-material/FilterAltOutlined";
import SwapVertOutlinedIcon from "@mui/icons-material/SwapVertOutlined";
import ViewListIcon from "@mui/icons-material/ViewList";
import GridViewIcon from "@mui/icons-material/GridView";
import { useEffect, useState } from "react";
import Grid from "@mui/material/Grid";
import GridSubscriptionCard from "../components/GridSubscriptionCard";
import SubscriptionForm from "../components/SubscriptionForm";
import { ToggleButtonGroup, ToggleButton } from "@mui/material";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import type { AppDispatch, RootState } from "../app/store";
import { fetchSubscriptions } from "../app/subscriptionSlice";
import { parseDecimal } from "../utils/money";
import AddIcon from "@mui/icons-material/Add";

export default function Subscriptions() {
  const { t } = useTranslation();
  const [view, setView] = useState<"grid" | "list">("list");
  const dispatch = useDispatch<AppDispatch>();
  const [searchQuery, setSearchQuery] = useState("");
  const [filterCategory, setFilterCategory] = useState("all");
  const [sortBy, setSortBy] = useState("name");
  const [isAddFormOpen, setIsAddFormOpen] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const handleEdit = (id: number) => {
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
          return parseDecimal(b.cost) > parseDecimal(a.cost) ? 1 : parseDecimal(b.cost) < parseDecimal(a.cost) ? -1 : 0;
        case "nextBilling":
          return (a.nextBillingDate ? new Date(a.nextBillingDate).getTime() : Number.POSITIVE_INFINITY)
            - (b.nextBillingDate ? new Date(b.nextBillingDate).getTime() : Number.POSITIVE_INFINITY);
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
        <Stack
          direction={{ xs: "column", md: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "stretch", md: "center" }}
          spacing={{ xs: 2, md: 0 }}
          mb={3}
        >
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setIsAddFormOpen(true)}
            sx={{
              display: { xs: "inline-flex", md: "none" },
              minHeight: 44,
              textTransform: "none",
            }}
          >
            {t("subscriptions.add", "Add Subscription")}
          </Button>
          <TextField
            placeholder={t("subscriptions.search", "Search subscriptions...")}
            size="small"
            sx={{ width: { xs: "100%", md: 300 } }}
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

          <Stack
            direction="row"
            spacing={1}
            useFlexGap
            flexWrap={{ xs: "wrap", md: "nowrap" }}
            sx={{
              width: { xs: "100%", md: "auto" },
              "& > .MuiButton-root": {
                minHeight: { xs: 44, md: "auto" },
              },
            }}
          >
            <Button
              variant="outlined"
              onClick={(e) => setFilterAnchorEl(e.currentTarget)}
              startIcon={<FilterAltOutlinedIcon fontSize="small" />}
              sx={{ flex: { xs: "1 1 auto", md: "0 0 auto" } }}
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
              sx={{ flex: { xs: "1 1 auto", md: "0 0 auto" } }}
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
              sx={{
                ml: { xs: "auto", md: 0 },
                "& .MuiToggleButton-root": {
                  minWidth: { xs: 44, md: "auto" },
                  minHeight: { xs: 44, md: "auto" },
                },
              }}
            >
              <ToggleButton value="list" aria-label="List view">
                <ViewListIcon fontSize="small" />
              </ToggleButton>

              <ToggleButton value="grid" aria-label="Grid view">
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
