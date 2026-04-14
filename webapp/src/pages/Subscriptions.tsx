import { Box, TextField, InputAdornment, Button, Stack, Typography } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import SubscriptionCard from "../components/SubscriptionCard";
import FilterAltOutlinedIcon from "@mui/icons-material/FilterAltOutlined";
import SwapVertOutlinedIcon from "@mui/icons-material/SwapVertOutlined";
import ViewListIcon from "@mui/icons-material/ViewList";
import GridViewIcon from "@mui/icons-material/GridView";
import { useState } from "react";
import Grid from "@mui/material/Grid";

export default function Subscriptions() {
  const [view, setView] = useState<"grid" | "list">("list");
  console.log("Current view:", view);
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
          {[1, 2, 3, 4].map((item) => (
            <Grid
              key={item}
              size={{
                xs: 12,
                sm: view === "grid" ? 6 : 12,
                md: view === "grid" ? 4 : 12,
                lg: view === "grid" ? 3 : 12,
              }}
            >
              <SubscriptionCard view={view} />
            </Grid>
          ))}
        </Grid>
      </Box>
    </Box>
  );
}
