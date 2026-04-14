import { Box, Typography, Chip, Stack, Avatar, Button, Switch } from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import { deepOrange } from "@mui/material/colors";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import AttachMoneyIcon from "@mui/icons-material/AttachMoney";
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined";
import QueryBuilderOutlinedIcon from "@mui/icons-material/QueryBuilderOutlined";
import LanguageOutlinedIcon from "@mui/icons-material/LanguageOutlined";

interface Props {
  view?: "grid" | "list";
}

export default function SubscriptionCard({ view = "list" }: Props) {
  const isGrid = view === "grid";

  return (
    <Box
      sx={{
        p: 3,
        borderRadius: 3,
        border: "1px solid",
        borderColor: "divider",
        maxWidth: "1200px",
      }}
    >
      <Box
        display="flex"
        flexDirection={isGrid ? "column" : "row"}
        justifyContent="space-between"
        alignItems={isGrid ? "flex-start" : "flex-start"}
        gap={isGrid ? 2 : 0}
      >
        <Stack direction={isGrid ? "column" : "row"} spacing={2}>
          <Avatar variant="rounded" sx={{ bgcolor: deepOrange[500], width: 48, height: 48 }}>
            A
          </Avatar>

          <Box>
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
              <Typography fontWeight="bold">Adobe Creative Cloud</Typography>
              <Chip label="active" size="small" color="success" />
            </Stack>
            {!isGrid && (
              <Typography variant="body2" color="text.secondary" mt={0.5}>
                Complete suite of creative apps...
              </Typography>
            )}
          </Box>
        </Stack>

        {/* Hide complex toggle in small grid view to save space if desired */}
        <Box textAlign={isGrid ? "left" : "right"}>
          <Typography variant="caption" display="flex" alignItems="center" gap={1}>
            Auto-renew: <Switch size="small" defaultChecked />
          </Typography>
        </Box>
      </Box>

      {/* Details Section */}
      <Box
        mt={3}
        display="grid"
        // Logic: 1 column for Grid view, 2 columns for List view
        gridTemplateColumns={isGrid ? "1fr" : "1fr 1fr"}
        gap={2}
      >
        <Box>
          <Stack direction="row" spacing={1} alignItems="center">
            <AttachMoneyIcon fontSize="small" />
            <Typography>
              <b>$54.99</b> / month
            </Typography>
          </Stack>
          {/* ... other info ... */}
        </Box>

        <Box>
          <Stack direction="row" spacing={1} alignItems="center">
            <CalendarMonthOutlinedIcon fontSize="small" />
            <Typography variant="body2">Next: Apr 20, 2026</Typography>
          </Stack>

          <Box display="flex" justifyContent="space-between" alignItems="center" mt={isGrid ? 2 : 1}>
            <Button size="small" variant="outlined" fullWidth={isGrid}>
              Actions
            </Button>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
