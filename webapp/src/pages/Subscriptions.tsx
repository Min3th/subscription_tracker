import { Box, TextField, InputAdornment, Button, Stack, Typography } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import SubscriptionCard from "../components/SubscriptionCard";

export default function Subscriptions() {
  return (
    <Box p={3}>
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
          <Button variant="outlined">All Categories</Button>
          <Button variant="outlined">Sort</Button>
        </Stack>
      </Stack>

      {/* Cards */}
      <Stack spacing={2}>
        <SubscriptionCard />
        <SubscriptionCard />
      </Stack>
    </Box>
  );
}
