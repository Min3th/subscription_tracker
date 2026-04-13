import { Box, Typography, Chip, Stack, Avatar, Button, Divider } from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVert";

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
      {/* LEFT SIDE */}
      <Stack direction="row" spacing={2}>
        <Avatar
          sx={{
            bgcolor: "linear-gradient(45deg, #6a5af9, #a855f7)",
            width: 48,
            height: 48,
          }}
        >
          A
        </Avatar>

        <Box>
          {/* Title + Tags */}
          <Stack direction="row" spacing={1} alignItems="center">
            <Typography fontWeight="bold">Adobe Creative Cloud</Typography>

            <Chip label="active" size="small" color="success" />
            <Chip label="Productivity" size="small" />
          </Stack>

          {/* Description */}
          <Typography variant="body2" color="text.secondary" mt={0.5}>
            Complete suite of creative apps including Photoshop, Illustrator, and Premiere Pro
          </Typography>

          {/* Price */}
          <Typography mt={1}>
            <b>$54.99</b> / month
          </Typography>

          {/* Payment */}
          <Typography variant="body2" color="text.secondary">
            Visa •••• 4242
          </Typography>

          <Typography variant="body2" color="text.secondary">
            Total Paid: $1649.76
          </Typography>
        </Box>
      </Stack>

      {/* RIGHT SIDE */}
      <Box textAlign="right">
        <Typography variant="body2">
          Auto-renew: <b>ON</b>
        </Typography>

        <Typography variant="body2" mt={1}>
          Next: Apr 20, 2026
        </Typography>

        <Typography variant="body2">Started: Aug 20, 2023</Typography>

        <Typography variant="body2" color="primary" sx={{ cursor: "pointer", mt: 1 }}>
          Visit website
        </Typography>

        <Button size="small" variant="outlined" startIcon={<MoreVertIcon />} sx={{ mt: 1 }}>
          Actions
        </Button>
      </Box>
    </Box>
  );
}
