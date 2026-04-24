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
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";

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
      <Box
        sx={{
          position: "relative",
          pt: { xs: 6, md: 8 },
          pb: { xs: 10, md: 15 },
          overflow: "hidden",
          background:
            theme.palette.mode === "dark"
              ? "radial-gradient(circle at 50% 50%, rgba(100, 0, 239, 0.1) 0%, transparent 60%)"
              : "radial-gradient(circle at 50% 50%, rgba(100, 0, 239, 0.05) 0%, transparent 60%)",
        }}
      >
        <style>
          {`
            @keyframes float {
              0% { transform: translateY(0px); }
              50% { transform: translateY(-20px); }
              100% { transform: translateY(0px); }
            }
            @keyframes float-reverse {
              0% { transform: translateY(0px); }
              50% { transform: translateY(20px); }
              100% { transform: translateY(0px); }
            }
          `}
        </style>

        {/* Floating Background Icons */}
        {[
          {
            src: "https://cdn.simpleicons.org/netflix/E50914",
            top: "15%",
            left: "15%",
            size: 80,
            delay: "0s",
            animation: "float",
            rotate: -10,
          },
          {
            src: "https://cdn.simpleicons.org/spotify/1ED760",
            top: "50%",
            left: "10%",
            size: 70,
            delay: "1s",
            animation: "float-reverse",
            rotate: 5,
          },
          {
            src: "https://cdn.simpleicons.org/patreon/FF424D",
            top: "80%",
            left: "20%",
            size: 90,
            delay: "2s",
            animation: "float",
            rotate: -5,
          },
          {
            src: "https://cdn.simpleicons.org/youtube/FF0000",
            top: "15%",
            right: "15%",
            size: 80,
            delay: "0.5s",
            animation: "float-reverse",
            rotate: 10,
          },
          {
            src: "https://cdn.simpleicons.org/applemusic/FA243C",
            top: "45%",
            right: "8%",
            size: 70,
            delay: "1.5s",
            animation: "float",
            rotate: -15,
          },
          {
            src: "https://cdn.simpleicons.org/twitch/9146FF",
            top: "75%",
            right: "20%",
            size: 90,
            delay: "2.5s",
            animation: "float-reverse",
            rotate: 5,
          },
          // Faded ones
          {
            src: "https://cdn.simpleicons.org/twitch/9146FF",
            top: "60%",
            left: "25%",
            size: 50,
            delay: "1.2s",
            animation: "float",
            rotate: 15,
            blur: 2,
            opacity: 0.6,
          },
          {
            src: "https://cdn.simpleicons.org/dropbox/0061FF",
            top: "25%",
            right: "25%",
            size: 60,
            delay: "0.8s",
            animation: "float-reverse",
            rotate: -20,
            blur: 3,
            opacity: 0.5,
          },
        ].map((icon, i) => (
          <Box
            key={i}
            sx={{
              position: "absolute",
              top: icon.top,
              left: icon.left,
              right: icon.right,
              animation: `${icon.animation} 6s ease-in-out infinite`,
              animationDelay: icon.delay,
              filter: icon.blur ? `blur(${icon.blur}px)` : "none",
              opacity: icon.opacity || 1,
              zIndex: 0,
              display: { xs: "none", md: "flex" },
            }}
          >
            <Box
              sx={{
                width: icon.size,
                height: icon.size,
                backgroundColor: theme.palette.mode === "dark" ? "#1e1e1e" : "white",
                borderRadius: "24%",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                boxShadow:
                  theme.palette.mode === "dark" ? "0 10px 30px rgba(0,0,0,0.5)" : "0 10px 30px rgba(0,0,0,0.08)",
                transform: `rotate(${icon.rotate}deg)`,
                "& img": {
                  width: "60%",
                  height: "60%",
                  objectFit: "contain",
                },
              }}
            >
              <img src={icon.src} alt="icon" />
            </Box>
          </Box>
        ))}

        {/* Floating Decorative Dots */}
        {[
          { top: "20%", left: "30%", color: "#6400ef", size: 10 },
          { top: "70%", left: "15%", color: "#d880f1", size: 15 },
          { top: "30%", right: "20%", color: "#6400ef", size: 12 },
          { top: "80%", right: "30%", color: "#d880f1", size: 8 },
          { top: "50%", right: "40%", color: "#6400ef", size: 14 },
        ].map((dot, i) => (
          <Box
            key={`dot-${i}`}
            sx={{
              position: "absolute",
              top: dot.top,
              left: dot.left,
              right: dot.right,
              width: dot.size,
              height: dot.size,
              borderRadius: "50%",
              backgroundColor: dot.color,
              opacity: 0.4,
              filter: "blur(2px)",
              zIndex: 0,
              display: { xs: "none", md: "block" },
            }}
          />
        ))}

        <Container maxWidth="md" sx={{ position: "relative", zIndex: 1 }}>
          <Stack spacing={4} alignItems="center" justifyContent="center" sx={{ textAlign: "center" }}>
            <Box
              sx={{
                display: "inline-flex",
                alignItems: "center",
                gap: 1,
                px: 2.5,
                py: 1,
                borderRadius: "999px",
                backgroundColor: theme.palette.mode === "dark" ? "rgba(100, 0, 239, 0.15)" : "rgba(100, 0, 239, 0.05)",
                border: `1px solid rgba(100, 0, 239, 0.2)`,
                mb: 1,
              }}
            >
              <AutoAwesomeIcon sx={{ fontSize: 18, color: theme.palette.purpink }} />
              <Typography variant="body2" sx={{ color: theme.palette.purpink, fontWeight: 600 }}>
                Stay in control of your subscriptions
              </Typography>
            </Box>

            <Typography
              variant="h2"
              sx={{ fontWeight: 800, color: "text.primary", fontSize: { xs: "3rem", md: "4.5rem" }, lineHeight: 1.1 }}
            >
              Track Subscriptions
              <br />
              <Box component="span" sx={{ color: theme.palette.purpink }}>
                Effortlessly
              </Box>
            </Typography>
            <Typography variant="h6" sx={{ maxWidth: "600px", color: "text.secondary", fontWeight: 400, mt: 1 }}>
              Get notified before your trials end. Avoid unexpected charges and stay in control of your subscriptions in
              one central place.
            </Typography>

            <Box sx={{ mt: 2 }}>
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
            </Box>

            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ fontSize: "0.9rem", display: "flex", alignItems: "center", gap: 1, mt: 1 }}
            >
              <Box component="span" sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                <AutoAwesomeIcon sx={{ fontSize: 14, color: "text.disabled" }} /> No ads
              </Box>
              •
              <Box component="span" sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                <AutoAwesomeIcon sx={{ fontSize: 14, color: "text.disabled" }} /> No credit card required
              </Box>
              •
              <Box component="span" sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                <AutoAwesomeIcon sx={{ fontSize: 14, color: "text.disabled" }} /> 100% free
              </Box>
            </Typography>
          </Stack>
        </Container>
      </Box>

      {/* WHY YOU NEED THIS SECTION */}
      <Box
        id="why-you-need"
        sx={{ py: 10, backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.02)" : "#f8f9fa" }}
      >
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
          <Grid container spacing={4} justifyContent="center">
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
              <Grid item xs={12} sm={6} md={4} key={i}>
                <Card
                  sx={{
                    height: "100%",
                    borderRadius: 4,
                    width: "300px",
                    boxShadow:
                      theme.palette.mode === "dark" ? "0 4px 20px rgba(0,0,0,0.4)" : "0 4px 20px rgba(0,0,0,0.05)",
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    textAlign: "center",
                    p: 4,
                    backgroundColor: theme.palette.mode === "dark" ? "background.paper" : "#ffffff",
                    transition: "transform 0.3s ease-in-out, box-shadow 0.3s ease-in-out",
                    "&:hover": {
                      transform: "translateY(-5px)",
                      boxShadow:
                        theme.palette.mode === "dark" ? "0 8px 30px rgba(0,0,0,0.6)" : "0 8px 30px rgba(0,0,0,0.1)",
                    },
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
      <Box
        id="everthing-you-need"
        sx={{
          py: 10,
          backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.02)" : "#f8f9fa",
          mt: "20px",
        }}
      >
        <Container id="features" maxWidth="lg" sx={{ py: { xs: 10, md: 15 } }}>
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
          <Grid container spacing={4} alignItems="center" justifyContent="center">
            {[
              {
                icon: <NotificationsActiveIcon sx={{ fontSize: 32, color: theme.palette.purpink }} />,
                title: "Smart Notifications",
                desc: "Get email or push alerts days before your card is charged, giving you plenty of time to cancel or renew.",
              },
              {
                icon: <BarChartIcon sx={{ fontSize: 32, color: theme.palette.purpink }} />,
                title: "Visual Insights",
                desc: "Understand your spending habits with intuitive charts. See your monthly and yearly costs broken down by category.",
              },
              {
                icon: <DashboardIcon sx={{ fontSize: 32, color: theme.palette.purpink }} />,
                title: "Centralized Dashboard",
                desc: "See all your active, paused, and cancelled subscriptions in one clean, easy-to-use dashboard.",
              },
              {
                icon: <PublicIcon sx={{ fontSize: 32, color: theme.palette.purpink }} />,
                title: "Flexible Billing Cycles",
                desc: "Whether you're billed weekly, monthly, or yearly, our tracker handles it all effortlessly.",
              },
            ].map((feature, i) => (
              <Grid item xs={12} sm={6} key={i}>
                <Card
                  sx={{
                    height: "300px",
                    width: "400px",
                    borderRadius: 4,
                    boxShadow:
                      theme.palette.mode === "dark" ? "0 4px 20px rgba(0,0,0,0.4)" : "0 4px 20px rgba(0,0,0,0.05)",
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    textAlign: "center",
                    p: 4,
                    backgroundColor: theme.palette.mode === "dark" ? "background.paper" : "#ffffff",
                    transition: "transform 0.3s ease-in-out, box-shadow 0.3s ease-in-out",
                    "&:hover": {
                      transform: "translateY(-5px)",
                      boxShadow:
                        theme.palette.mode === "dark" ? "0 8px 30px rgba(0,0,0,0.6)" : "0 8px 30px rgba(0,0,0,0.1)",
                    },
                  }}
                >
                  <Box
                    sx={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      width: 72,
                      height: 72,
                      borderRadius: "20px",
                      backgroundColor:
                        theme.palette.mode === "dark" ? "rgba(100, 0, 239, 0.2)" : "rgba(100, 0, 239, 0.1)",
                      mb: 3,
                    }}
                  >
                    {feature.icon}
                  </Box>
                  <Typography variant="h6" fontWeight="bold" gutterBottom>
                    {feature.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {feature.desc}
                  </Typography>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>
      {/* HOW IT WORKS SECTION */}
      <Box
        id="how-it-works"
        sx={{
          py: 10,
          backgroundColor: theme.palette.mode === "dark" ? "rgba(255,255,255,0.02)" : "#f8f9fa",
          mt: "20px",
        }}
      >
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
          <Grid container spacing={4} justifyContent="center">
            {[
              { step: "1", title: "Add Services", desc: "Enter your subscriptions and billing details in seconds." },
              { step: "2", title: "Set Reminders", desc: "Choose when you want to be notified before a charge." },
              {
                step: "3",
                title: "Stay in Control",
                desc: "Relax knowing you'll never miss a payment or cancellation again.",
              },
            ].map((item, i) => (
              <Grid item xs={12} sm={6} md={4} key={i}>
                <Card
                  sx={{
                    height: "100%",
                    width: "400px",
                    borderRadius: 4,
                    boxShadow: "none",
                    border: `1px solid ${theme.palette.divider}`,
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    textAlign: "center",
                    p: 4,
                    backgroundColor: "transparent",
                    transition: "transform 0.3s ease-in-out, box-shadow 0.3s ease-in-out",
                    "&:hover": {
                      transform: "translateY(-5px)",
                      boxShadow:
                        theme.palette.mode === "dark" ? "0 8px 30px rgba(0,0,0,0.6)" : "0 8px 30px rgba(0,0,0,0.05)",
                      borderColor: "transparent",
                    },
                  }}
                >
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
                      mb: 3,
                    }}
                  >
                    {item.step}
                  </Box>
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
