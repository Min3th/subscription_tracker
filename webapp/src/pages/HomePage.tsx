import { useState } from "react";
import { Box, Button, Typography, Container, Stack, Grid, Card, useTheme } from "@mui/material";
import Navbar from "../components/Navbar";
import LoginDialog from "../components/LoginDialog";

// MUI Icons
import NotificationsActiveIcon from "@mui/icons-material/NotificationsActive";
import BarChartIcon from "@mui/icons-material/BarChart";
import DashboardIcon from "@mui/icons-material/Dashboard";
import PublicIcon from "@mui/icons-material/Public";
import MoneyOffIcon from "@mui/icons-material/MoneyOff";
import EventBusyIcon from "@mui/icons-material/EventBusy";
import AccountTreeIcon from "@mui/icons-material/AccountTree";

export default function HomePage() {
  const theme = useTheme();
  const [loginOpen, setLoginOpen] = useState(false);

  const handleRegisterClick = () => {
    setLoginOpen(true);
  };

  return (
    <Box sx={{ minHeight: "100vh", backgroundColor: "background.default" }}>
      <Navbar open={false} onClick={() => {}} />
      <LoginDialog open={loginOpen} onClose={() => setLoginOpen(false)} />

      {/* HERO SECTION */}
      <Box sx={{ pt: { xs: 15, md: 20 }, pb: { xs: 10, md: 15 } }}>
        <Container maxWidth="md">
          <Stack spacing={4} alignItems="center" justifyContent="center" sx={{ textAlign: "center" }}>
            <Typography variant="h2" sx={{ fontWeight: 800, color: "#6400ef", fontSize: { xs: "2.5rem", md: "4rem" } }}>
              Track Subscriptions
              <br />
              Effortlessly
            </Typography>
            <Typography variant="h6" sx={{ maxWidth: "600px", color: "text.secondary", fontWeight: 400 }}>
              Get notified before your trials end. Avoid unexpected charges and stay in control of your subscriptions in
              one central place.
            </Typography>
            <Button
              onClick={handleRegisterClick}
              variant="contained"
              size="large"
              sx={{
                px: 5,
                py: 2,
                borderRadius: "999px",
                background: "linear-gradient(135deg, #6400ef, #d880f1)",
                boxShadow: "0 8px 20px rgba(100, 0, 239, 0.3)",
                textTransform: "none",
                fontSize: "1.2rem",
                fontWeight: 600,
                "&:hover": {
                  background: "linear-gradient(135deg, #5000c0, #c468dd)",
                  boxShadow: "0 10px 25px rgba(100, 0, 239, 0.4)",
                },
              }}
            >
              Register Now
            </Button>
            <Typography variant="caption" color="text.secondary" sx={{ fontSize: "0.9rem" }}>
              No ads • No credit card required • 100% free
            </Typography>
          </Stack>
        </Container>
      </Box>

      {/* WHY YOU NEED THIS SECTION */}
      <Box sx={{ py: 10, backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.02)" : "#f8f9fa" }}>
        <Container maxWidth="lg">
          <Typography
            variant="h3"
            sx={{
              textAlign: "center",
              fontWeight: 700,
              mb: 6,
              color: "text.primary",
              fontSize: { xs: "2rem", md: "3rem" },
            }}
          >
            Why You Need a Tracker
          </Typography>
          <Grid container spacing={4}>
            {[
              {
                icon: <EventBusyIcon sx={{ fontSize: 40, color: "#f44336" }} />,
                title: "Forgotten Free Trials",
                desc: "We've all been there. You sign up for a 7-day trial and forget to cancel, ending up paying for a service you don't even use.",
              },
              {
                icon: <MoneyOffIcon sx={{ fontSize: 40, color: "#ff9800" }} />,
                title: "Wasted Money",
                desc: "Unused subscriptions drain your bank account silently. Identifying and cancelling them can save you hundreds of dollars yearly.",
              },
              {
                icon: <AccountTreeIcon sx={{ fontSize: 40, color: "#2196f3" }} />,
                title: "Subscription Fatigue",
                desc: "With streaming, software, and delivery services, it's impossible to keep track of every billing cycle manually.",
              },
            ].map((item, i) => (
              <Grid item xs={12} md={4} key={i}>
                <Card
                  sx={{
                    height: "100%",
                    borderRadius: 4,
                    boxShadow:
                      theme.palette.mode === "dark" ? "0 4px 20px rgba(0,0,0,0.4)" : "0 4px 20px rgba(0,0,0,0.05)",
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    textAlign: "center",
                    p: 4,
                    backgroundColor: theme.palette.mode === "dark" ? "background.paper" : "#ffffff",
                  }}
                >
                  <Box sx={{ mb: 2 }}>{item.icon}</Box>
                  <Typography variant="h6" fontWeight="bold" gutterBottom>
                    {item.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {item.desc}
                  </Typography>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* FEATURES SECTION */}
      <Container maxWidth="lg" sx={{ py: { xs: 10, md: 15 } }}>
        <Typography
          variant="h3"
          sx={{
            textAlign: "center",
            fontWeight: 700,
            mb: 8,
            color: "text.primary",
            fontSize: { xs: "2rem", md: "3rem" },
          }}
        >
          Everything You Need
        </Typography>
        <Grid container spacing={6} alignItems="center">
          {[
            {
              icon: <NotificationsActiveIcon sx={{ fontSize: 32, color: "#6400ef" }} />,
              title: "Smart Notifications",
              desc: "Get email or push alerts days before your card is charged, giving you plenty of time to cancel or renew.",
            },
            {
              icon: <BarChartIcon sx={{ fontSize: 32, color: "#6400ef" }} />,
              title: "Visual Insights",
              desc: "Understand your spending habits with intuitive charts. See your monthly and yearly costs broken down by category.",
            },
            {
              icon: <DashboardIcon sx={{ fontSize: 32, color: "#6400ef" }} />,
              title: "Centralized Dashboard",
              desc: "See all your active, paused, and cancelled subscriptions in one clean, easy-to-use dashboard.",
            },
            {
              icon: <PublicIcon sx={{ fontSize: 32, color: "#6400ef" }} />,
              title: "Flexible Billing Cycles",
              desc: "Whether you're billed weekly, monthly, or yearly, our tracker handles it all effortlessly.",
            },
          ].map((feature, i) => (
            <Grid item xs={12} sm={6} key={i}>
              <Stack direction="row" spacing={3}>
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    width: 64,
                    height: 64,
                    borderRadius: "16px",
                    backgroundColor:
                      theme.palette.mode === "dark" ? "rgba(100, 0, 239, 0.2)" : "rgba(100, 0, 239, 0.1)",
                    flexShrink: 0,
                  }}
                >
                  {feature.icon}
                </Box>
                <Box>
                  <Typography variant="h6" fontWeight="bold" gutterBottom>
                    {feature.title}
                  </Typography>
                  <Typography variant="body1" color="text.secondary">
                    {feature.desc}
                  </Typography>
                </Box>
              </Stack>
            </Grid>
          ))}
        </Grid>
      </Container>

      {/* HOW IT WORKS SECTION */}
      <Box sx={{ py: 10, backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.02)" : "#f8f9fa" }}>
        <Container maxWidth="md">
          <Typography
            variant="h3"
            sx={{
              textAlign: "center",
              fontWeight: 700,
              mb: 8,
              color: "text.primary",
              fontSize: { xs: "2rem", md: "3rem" },
            }}
          >
            How It Works
          </Typography>
          <Grid container spacing={4}>
            {[
              { step: "1", title: "Add Services", desc: "Enter your subscriptions and billing details in seconds." },
              { step: "2", title: "Set Reminders", desc: "Choose when you want to be notified before a charge." },
              {
                step: "3",
                title: "Stay in Control",
                desc: "Relax knowing you'll never miss a payment or cancellation again.",
              },
            ].map((item, i) => (
              <Grid item xs={12} md={4} key={i}>
                <Stack alignItems="center" textAlign="center" spacing={2}>
                  <Box
                    sx={{
                      width: 56,
                      height: 56,
                      borderRadius: "50%",
                      background: "linear-gradient(135deg, #6400ef, #d880f1)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      color: "white",
                      fontSize: "1.5rem",
                      fontWeight: "bold",
                      boxShadow: "0 4px 14px rgba(100, 0, 239, 0.4)",
                    }}
                  >
                    {item.step}
                  </Box>
                  <Typography variant="h6" fontWeight="bold">
                    {item.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {item.desc}
                  </Typography>
                </Stack>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* FINAL CTA SECTION */}
      <Container maxWidth="md" sx={{ py: { xs: 10, md: 15 }, textAlign: "center" }}>
        <Typography variant="h3" sx={{ fontWeight: 700, mb: 3, fontSize: { xs: "2rem", md: "3rem" } }}>
          Ready to save money?
        </Typography>
        <Typography variant="h6" color="text.secondary" sx={{ mb: 5, fontWeight: 400 }}>
          Join thousands of users who have taken back control of their subscriptions.
        </Typography>
        <Button
          onClick={handleRegisterClick}
          variant="contained"
          size="large"
          sx={{
            px: 6,
            py: 2,
            borderRadius: "999px",
            background: "linear-gradient(135deg, #6400ef, #d880f1)",
            boxShadow: "0 8px 20px rgba(100, 0, 239, 0.3)",
            textTransform: "none",
            fontSize: "1.2rem",
            fontWeight: 600,
            "&:hover": {
              background: "linear-gradient(135deg, #5000c0, #c468dd)",
              boxShadow: "0 10px 25px rgba(100, 0, 239, 0.4)",
            },
          }}
        >
          Start Tracking for Free
        </Button>
      </Container>
    </Box>
  );
}
