import { Box, Typography, Link } from "@mui/material";

export default function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        width: "100%",
        py: 2,
        borderTop: "1px solid",
        borderColor: "divider",
        backgroundColor: "background.paper",
        textAlign: "center",
      }}
    >
      <Typography variant="body2" color="text.secondary">
        © {new Date().getFullYear()} Your App Name. All rights reserved.
      </Typography>

      <Box sx={{ mt: 1 }}>
        <Link href="#" underline="hover" sx={{ mx: 1 }}>
          Privacy
        </Link>
        <Link href="#" underline="hover" sx={{ mx: 1 }}>
          Terms
        </Link>
        <Link href="#" underline="hover" sx={{ mx: 1 }}>
          Contact
        </Link>
      </Box>
    </Box>
  );
}
