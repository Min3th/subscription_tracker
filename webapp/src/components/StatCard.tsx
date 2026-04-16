import { Box, Card, Stack, Typography, alpha, useTheme } from "@mui/material";

export default function StatCard({ title, value, icon }: any) {
  const theme = useTheme();
  return (
    <Card
      sx={{
        borderRadius: 3,
        p: 3,
        boxShadow: "0 4px 20px rgba(0,0,0,0.04)",
        transition: "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
        "&:hover": {
          transform: "translateY(-4px)",
          boxShadow: "0 8px 25px rgba(0,0,0,0.1)",
        },
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
      }}
    >
      <Stack spacing={2} alignItems="flex-start">
        <Box display="flex" justifyContent="space-between" alignItems="center" width="100%">
          <Typography variant="subtitle2" color="text.secondary" fontWeight="600">
            {title}
          </Typography>
          <Box sx={{ 
            color: theme.palette.primary.main, 
            display: "flex",
            bgcolor: alpha(theme.palette.primary.main, 0.1),
            p: 1,
            borderRadius: 2,
          }}>
            {icon}
          </Box>
        </Box>
        <Typography variant="h4" fontWeight="bold">
          {value}
        </Typography>
      </Stack>
    </Card>
  );
}
