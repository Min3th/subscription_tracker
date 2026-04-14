import { Box, TextField, InputAdornment, Button, Stack, Typography } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import SubscriptionCard from "../components/SubscriptionCard";
import FilterAltOutlinedIcon from "@mui/icons-material/FilterAltOutlined";
import SwapVertOutlinedIcon from "@mui/icons-material/SwapVertOutlined";

export default function Subscriptions() {
  return (
    <Box sx={{ width: "100%", display: "flex", justifyContent: "center" }}>
      <Box sx={{ width: "100%", maxWidth: 1100 }}>
        {/* Top Controls */}
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
          </Stack>
        </Stack>

        {/* Cards */}
        <Stack spacing={2}>
          <SubscriptionCard />
          <SubscriptionCard />
        </Stack>
      </Box>
    </Box>
  );
}
