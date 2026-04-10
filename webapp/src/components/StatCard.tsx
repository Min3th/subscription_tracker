import { Box, Card, Stack, Typography } from "@mui/material";

export default function StatCard({ title, value, icon }: any) {
  return (
    <Card
      sx={{
        borderRadius: 3,
        p: 2,
        boxShadow: "0 4px 20px rgba(0,0,0,0.08)",
        transition: "0.2s",
        "&:hover": {
          transform: "translateY(-4px)",
        },
        width: "250px",
      }}
    >
      <Stack spacing={0.5} alignItems="flex-start">
        {/* TOP ROW: Title and Icon pushed to opposite ends */}
        <Box display="flex" justifyContent="space-between" alignItems="center" width="100%">
          <Typography variant="body2" color="text.secondary" fontWeight="500">
            {title}
          </Typography>

          {/* Wrapper for the icon to ensure consistent coloring/sizing */}
          <Box sx={{ color: "primary.main", display: "flex" }}>{icon}</Box>
        </Box>

        {/* BOTTOM ROW: Value */}
        <Typography variant="h5" fontWeight="bold">
          {value}
        </Typography>
      </Stack>
    </Card>
  );
}
