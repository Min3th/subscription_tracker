import { Box, Typography, Chip, Stack, Avatar, Button, Divider } from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import { deepOrange } from "@mui/material/colors";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import AttachMoneyIcon from "@mui/icons-material/AttachMoney";
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined";
import QueryBuilderOutlinedIcon from "@mui/icons-material/QueryBuilderOutlined";
import LanguageOutlinedIcon from "@mui/icons-material/LanguageOutlined";

export default function SubscriptionCard() {
  return (
    <Box
      sx={{
        p: 2,
        borderRadius: 3,
        border: "1px solid",
        borderColor: "divider",
        display: "flex",
        justifyContent: "space-between",
        alignItems: "flex-start",
      }}
    >
      <Stack direction="row" spacing={2}>
        <Avatar
          sx={{
            bgcolor: deepOrange[500],
            width: 48,
            height: 48,
          }}
          variant="rounded"
        >
          A
        </Avatar>

        <Box sx={{ display: "flex", flexDirection: "column", alignItems: "start" }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <Typography fontWeight="bold">Adobe Creative Cloud</Typography>

            <Chip label="active" size="small" color="success" />
            <Chip label="Productivity" size="small" />
          </Stack>

          <Typography variant="body2" color="text.secondary" mt={0.5}>
            Complete suite of creative apps including Photoshop, Illustrator, and Premiere Pro
          </Typography>
          <Box sx={{ display: "flex", flexDirection: "row", justifyContent: "space-between", width: "100%", mt: 2 }}>
            <Box>
              <Typography mt={1}>
                <AttachMoneyIcon />
                <b>$54.99</b> / month
              </Typography>

              <Typography variant="body2" color="text.secondary">
                <CreditCardIcon />
                Visa •••• 4242
              </Typography>

              <Typography variant="body2" color="text.secondary">
                <TrendingUpIcon />
                Total Paid: $1649.76
              </Typography>
            </Box>
            <Box>
              <Typography variant="body2" mt={1}>
                <CalendarMonthOutlinedIcon />
                Next: Apr 20, 2026
              </Typography>

              <Typography variant="body2">
                <QueryBuilderOutlinedIcon />
                Started: Aug 20, 2023
              </Typography>

              <Typography variant="body2" color="primary" sx={{ cursor: "pointer", mt: 1 }}>
                <LanguageOutlinedIcon />
                Visit website
              </Typography>
            </Box>
          </Box>
        </Box>
      </Stack>
      <Box textAlign="right">
        <Typography variant="body2">
          Auto-renew: <b>ON</b>
        </Typography>
        <Button size="small" variant="outlined" startIcon={<MoreVertIcon />} sx={{ mt: 1 }}>
          Actions
        </Button>
      </Box>
    </Box>
  );
}
