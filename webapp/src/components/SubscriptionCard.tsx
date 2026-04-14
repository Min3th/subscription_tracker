import { Box, Typography, Chip, Stack, Avatar, Button, Switch } from "@mui/material";
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
        p: 3,
        borderRadius: 3,
        border: "1px solid",
        borderColor: "divider",
        maxWidth: "1200px",
      }}
    >
      <Box display="flex" justifyContent="space-between" alignItems="flex-start">
        <Stack direction="row" spacing={2}>
          <Avatar variant="rounded" sx={{ bgcolor: deepOrange[500], width: 48, height: 48 }}>
            A
          </Avatar>

          <Box>
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography fontWeight="bold">Adobe Creative Cloud</Typography>

              <Chip label="active" size="small" color="success" />
              <Chip label="Productivity" size="small" />
            </Stack>
            <Typography variant="body2" color="text.secondary" mt={0.5}>
              Complete suite of creative apps including Photoshop, Illustrator, and Premiere Pro
            </Typography>
          </Box>
        </Stack>

        <Box textAlign="right">
          <Typography variant="body2" display="flex" alignItems="center" gap={1}>
            Auto-renew:
            <Switch defaultChecked />
          </Typography>
        </Box>
      </Box>

      <Box mt={3} display="grid" gridTemplateColumns="1fr 1fr" gap={2}>
        <Box>
          <Stack direction="row" spacing={1} alignItems="center">
            <AttachMoneyIcon fontSize="small" />
            <Typography>
              <b>$54.99</b> / month
            </Typography>
          </Stack>

          <Stack direction="row" spacing={1} alignItems="center" mt={1}>
            <CreditCardIcon fontSize="small" />
            <Typography variant="body2" color="text.secondary">
              Visa •••• 4242
            </Typography>
          </Stack>

          <Stack direction="row" spacing={1} alignItems="center" mt={1}>
            <TrendingUpIcon fontSize="small" />
            <Typography variant="body2" color="text.secondary">
              Total Paid: $1649.76
            </Typography>
          </Stack>
        </Box>

        <Box>
          <Stack direction="row" spacing={1} alignItems="center">
            <CalendarMonthOutlinedIcon fontSize="small" />
            <Typography variant="body2">Next: Apr 20, 2026</Typography>
          </Stack>

          <Stack direction="row" spacing={1} alignItems="center" mt={1}>
            <QueryBuilderOutlinedIcon fontSize="small" />
            <Typography variant="body2">Started: Aug 20, 2023</Typography>
          </Stack>

          <Box display="flex" justifyContent="space-between" alignItems="center" mt={1}>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ cursor: "pointer" }}>
              <LanguageOutlinedIcon fontSize="small" color="primary" />
              <Typography variant="body2" color="primary">
                Visit website
              </Typography>
            </Stack>

            <Button
              sx={{
                color: "black",
                borderColor: "black",
                textTransform: "none",
              }}
              size="small"
              variant="outlined"
              startIcon={<MoreVertIcon />}
            >
              Actions
            </Button>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
