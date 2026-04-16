import { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Switch,
  FormControlLabel,
  Divider,
  Avatar,
  IconButton,
  Alert,
  Grid,
} from "@mui/material";
import PersonIcon from "@mui/icons-material/Person";
import NotificationsIcon from "@mui/icons-material/Notifications";
import PublicIcon from "@mui/icons-material/Public";
import PaletteIcon from "@mui/icons-material/Palette";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import PhotoCameraIcon from "@mui/icons-material/PhotoCamera";
import SaveIcon from "@mui/icons-material/Save";
import type { SelectChangeEvent } from "@mui/material";
import { useSelector } from "react-redux";
import type { RootState } from "../app/store";

export function Settings() {
  const { user, token } = useSelector((state: RootState) => state.auth);
  if (!user) {
    return <Typography>Loading...</Typography>;
  }
  const [formData, setFormData] = useState({
    name: user.name,
    email: user.email,
    currency: "USD",
    timezone: "America/New_York",
    theme: "light",
    language: "en",
  });

  const [notifications, setNotifications] = useState({
    emailNotifications: true,
    upcomingBilling: true,
    weeklyReport: false,
    renewalReminders: true,
    priceChanges: true,
  });

  const [privacy, setPrivacy] = useState({
    publicProfile: false,
    showEmail: false,
    dataCollection: true,
  });

  const [saved, setSaved] = useState(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSelectChange = (e: SelectChangeEvent) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleNotificationChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNotifications({
      ...notifications,
      [e.target.name]: e.target.checked,
    });
  };

  const handleSave = () => {
    console.log("Settings saved:", { formData, notifications, privacy });
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  const currencies = [
    { code: "USD", symbol: "$", name: "US Dollar" },
    { code: "EUR", symbol: "€", name: "Euro" },
    { code: "GBP", symbol: "£", name: "British Pound" },
    { code: "JPY", symbol: "¥", name: "Japanese Yen" },
    { code: "CAD", symbol: "C$", name: "Canadian Dollar" },
    { code: "AUD", symbol: "A$", name: "Australian Dollar" },
    { code: "INR", symbol: "₹", name: "Indian Rupee" },
  ];

  const timezones = [
    "America/New_York",
    "America/Chicago",
    "America/Denver",
    "America/Los_Angeles",
    "Europe/London",
    "Europe/Paris",
    "Asia/Tokyo",
    "Asia/Dubai",
    "Australia/Sydney",
  ];

  const themes = [
    { value: "light", label: "Light" },
    { value: "dark", label: "Dark" },
    { value: "auto", label: "Auto (System)" },
  ];

  const languages = [
    { code: "en", name: "English" },
    { code: "es", name: "Spanish" },
    { code: "fr", name: "French" },
    { code: "de", name: "German" },
    { code: "ja", name: "Japanese" },
    { code: "zh", name: "Chinese" },
  ];

  return (
    <Box
      display="flex"
      flexDirection="column"
      gap={3}
      sx={{ maxWidth: 1000, mx: "auto", p: 3 }}
      justifyContent="center"
    >
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: "bold" }}>
          Settings
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5 }}>
          Manage your account preferences and settings
        </Typography>
      </Box>

      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Settings saved successfully!
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <PersonIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Profile Information
                </Typography>
              </Box>

              <Box sx={{ display: "flex", alignItems: "center", gap: 3, mb: 3 }}>
                <Box sx={{ position: "relative" }}>
                  <Avatar src={user.picture} alt={user.name} sx={{ width: 100, height: 100 }} />
                  <IconButton
                    sx={{
                      position: "absolute",
                      bottom: -5,
                      right: -5,
                      bgcolor: "primary.main",
                      color: "white",
                      width: 36,
                      height: 36,
                      "&:hover": {
                        bgcolor: "primary.dark",
                      },
                    }}
                    size="small"
                  >
                    <PhotoCameraIcon sx={{ fontSize: 18 }} />
                  </IconButton>
                </Box>
                <Box>
                  <Typography variant="body1" sx={{ fontWeight: 500 }}>
                    Profile Photo
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    PNG, JPG up to 5MB
                  </Typography>
                </Box>
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Full Name"
                      name="name"
                      value={formData.name}
                      onChange={handleInputChange}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Email Address"
                      name="email"
                      type="email"
                      value={formData.email}
                      onChange={handleInputChange}
                    />
                  </Grid>
                </Grid>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Preferences */}
        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <PublicIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Preferences
                </Typography>
              </Box>

              <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 4 }}>
                  <FormControl fullWidth>
                    <InputLabel>Currency</InputLabel>
                    <Select name="currency" value={formData.currency} onChange={handleSelectChange} label="Currency">
                      {currencies.map((currency) => (
                        <MenuItem key={currency.code} value={currency.code}>
                          {currency.symbol} {currency.name} ({currency.code})
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid size={{ xs: 12, md: 4 }}>
                  <FormControl fullWidth>
                    <InputLabel>Language</InputLabel>
                    <Select name="language" value={formData.language} onChange={handleSelectChange} label="Language">
                      {languages.map((lang) => (
                        <MenuItem key={lang.code} value={lang.code}>
                          {lang.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid size={{ xs: 12, md: 4 }}>
                  <FormControl fullWidth>
                    <InputLabel>Timezone</InputLabel>
                    <Select name="timezone" value={formData.timezone} onChange={handleSelectChange} label="Timezone">
                      {timezones.map((tz) => (
                        <MenuItem key={tz} value={tz}>
                          {tz}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Appearance */}
        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <PaletteIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Appearance
                </Typography>
              </Box>

              <FormControl fullWidth>
                <InputLabel>Theme</InputLabel>
                <Select name="theme" value={formData.theme} onChange={handleSelectChange} label="Theme">
                  {themes.map((theme) => (
                    <MenuItem key={theme.value} value={theme.value}>
                      {theme.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: "block" }}>
                Choose how the app looks to you. Select a single theme, or sync with your system.
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Notifications */}
        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <NotificationsIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Notifications
                </Typography>
              </Box>

              <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <Box sx={{ textAlign: "left" }}>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      Email Notifications
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Receive email updates about your account
                    </Typography>
                  </Box>
                  <Switch
                    checked={notifications.emailNotifications}
                    onChange={handleNotificationChange}
                    name="emailNotifications"
                  />
                </Box>

                <Divider />

                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <Box sx={{ textAlign: "left" }}>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      Upcoming Billing Alerts
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Get notified 3 days before a subscription renews
                    </Typography>
                  </Box>
                  <Switch
                    checked={notifications.upcomingBilling}
                    onChange={handleNotificationChange}
                    name="upcomingBilling"
                  />
                </Box>

                <Divider />

                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <Box sx={{ textAlign: "left" }}>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      Renewal Reminders
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Reminders for upcoming subscription renewals
                    </Typography>
                  </Box>
                  <Switch
                    checked={notifications.renewalReminders}
                    onChange={handleNotificationChange}
                    name="renewalReminders"
                  />
                </Box>

                <Divider />

                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <Box sx={{ textAlign: "left" }}>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      Price Change Alerts
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Get notified when subscription prices change
                    </Typography>
                  </Box>
                  <Switch
                    checked={notifications.priceChanges}
                    onChange={handleNotificationChange}
                    name="priceChanges"
                  />
                </Box>

                <Divider />

                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <Box sx={{ textAlign: "left" }}>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      Weekly Summary Report
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Receive a weekly summary of your subscriptions
                    </Typography>
                  </Box>
                  <Switch
                    checked={notifications.weeklyReport}
                    onChange={handleNotificationChange}
                    name="weeklyReport"
                  />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <CreditCardIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Payment Methods
                </Typography>
              </Box>

              <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                <Box
                  sx={{
                    p: 2,
                    border: "1px solid",
                    borderColor: "divider",
                    borderRadius: 1,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                  }}
                >
                  <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                    <Box
                      sx={{
                        width: 48,
                        height: 32,
                        bgcolor: "#f5f5f5",
                        borderRadius: 0.5,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                      }}
                    >
                      <CreditCardIcon sx={{ color: "#1976d2" }} />
                    </Box>
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Visa ending in 4242
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Expires 12/2026
                      </Typography>
                    </Box>
                  </Box>
                  <Button variant="outlined" size="small">
                    Edit
                  </Button>
                </Box>

                <Button variant="outlined" fullWidth>
                  Add Payment Method
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      <Box sx={{ mt: 4, display: "flex", justifyContent: "flex-end", gap: 2 }}>
        <Button variant="outlined">Cancel</Button>
        <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave}>
          Save Changes
        </Button>
      </Box>
    </Box>
  );
}
