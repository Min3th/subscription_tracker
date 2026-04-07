import { Box, Button, Typography, Container, Stack, Chip } from "@mui/material";
import Navbar from "../components/Navbar";

export default function HomePage() {
  return (
    <Box sx={{ minHeight: "100vh", backgroundColor: "background.default" }}>
      <Navbar />

      {/* HERO SECTION */}
      <Container maxWidth="md">
        <Stack
          spacing={4}
          alignItems="center"
          justifyContent="center"
          sx={{
            minHeight: "90vh",
            textAlign: "center",
          }}
        >
          {/* Badge */}
          <Chip
            label="✨ 100% Free Forever"
            sx={{
              backgroundColor: "#E6F9F0",
              color: "#6400ef",
              fontWeight: 600,
            }}
          />

          {/* Main Heading */}
          <Typography
            variant="h2"
            sx={{
              fontWeight: 700,
              lineHeight: 1.2,
            }}
          >
            Never Miss a Free Trial Expiry
          </Typography>

          {/* Highlight text */}
          <Typography
            variant="h3"
            sx={{
              fontWeight: 700,
              color: "#6400ef",
            }}
          >
            Track Subscriptions Effortlessly
          </Typography>

          {/* Subtext */}
          <Typography
            variant="body1"
            sx={{
              maxWidth: "600px",
              color: "text.secondary",
            }}
          >
            Get notified before your trials end. Avoid unexpected charges and stay in control of your subscriptions.
          </Typography>

          {/* CTA Button */}
          <Button
            variant="contained"
            size="large"
            sx={{
              px: 4,
              py: 1.5,
              borderRadius: "999px",
              background: "linear-gradient(135deg, #6400ef, #d880f1)",
              boxShadow: "0 8px 20px rgba(100, 0, 239, 0.3)",
              textTransform: "none",
              fontSize: "1rem",
              "&:hover": {
                background: "linear-gradient(135deg, #6400ef, #d880f1)",
              },
            }}
          >
            Register Now!
          </Button>

          {/* Small text */}
          <Typography variant="caption" color="text.secondary">
            No ads • No credit card • 100% free
          </Typography>
        </Stack>
      </Container>
    </Box>
  );
}
