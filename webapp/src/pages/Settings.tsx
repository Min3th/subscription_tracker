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
import SecurityIcon from "@mui/icons-material/Security";
import PhotoCameraIcon from "@mui/icons-material/PhotoCamera";
import SaveIcon from "@mui/icons-material/Save";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import type { SelectChangeEvent } from "@mui/material";
import Palette from "@mui/icons-material/Palette";

interface SettingsProps {
  user: {
    name: string;
    email: string;
    photoUrl: string;
  };
  onBack: () => void;
}

export function Settings({ user, onBack }: SettingsProps) {
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

  const handlePrivacyChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPrivacy({
      ...privacy,
      [e.target.name]: e.target.checked,
    });
  };

  const handleSave = () => {
    // Save settings logic here
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
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={onBack}>
          Back
        </Button>
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
        {/* Profile Settings */}
        <Grid item xs={12}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <PersonIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Profile Information
                </Typography>
              </Box>

              <Box sx={{ display: "flex", alignItems: "center", gap: 3, mb: 3 }}>
                <Box sx={{ position: "relative" }}>
                  <Avatar src={user.photoUrl} alt={user.name} sx={{ width: 100, height: 100 }} />
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
              </Box>

              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Full Name"
                    name="name"
                    value={formData.name}
                    onChange={handleInputChange}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
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
            </CardContent>
          </Card>
        </Grid>

        {/* Preferences */}
        <Grid item xs={12}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <PublicIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Preferences
                </Typography>
              </Box>

              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
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

                <Grid item xs={12} sm={6}>
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

                <Grid item xs={12} sm={6}>
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
        <Grid item xs={12}>
          <Card>
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
        <Grid item xs={12}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <NotificationsIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Notifications
                </Typography>
              </Box>

              <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={notifications.emailNotifications}
                      onChange={handleNotificationChange}
                      name="emailNotifications"
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Email Notifications
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Receive email updates about your account
                      </Typography>
                    </Box>
                  }
                />

                <Divider />

                <FormControlLabel
                  control={
                    <Switch
                      checked={notifications.upcomingBilling}
                      onChange={handleNotificationChange}
                      name="upcomingBilling"
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Upcoming Billing Alerts
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Get notified 3 days before a subscription renews
                      </Typography>
                    </Box>
                  }
                />

                <Divider />

                <FormControlLabel
                  control={
                    <Switch
                      checked={notifications.renewalReminders}
                      onChange={handleNotificationChange}
                      name="renewalReminders"
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Renewal Reminders
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Reminders for upcoming subscription renewals
                      </Typography>
                    </Box>
                  }
                />

                <Divider />

                <FormControlLabel
                  control={
                    <Switch
                      checked={notifications.priceChanges}
                      onChange={handleNotificationChange}
                      name="priceChanges"
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Price Change Alerts
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Get notified when subscription prices change
                      </Typography>
                    </Box>
                  }
                />

                <Divider />

                <FormControlLabel
                  control={
                    <Switch
                      checked={notifications.weeklyReport}
                      onChange={handleNotificationChange}
                      name="weeklyReport"
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Weekly Summary Report
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Receive a weekly summary of your subscriptions
                      </Typography>
                    </Box>
                  }
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Privacy & Security */}
        <Grid item xs={12}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <SecurityIcon sx={{ color: "#1976d2" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  Privacy & Security
                </Typography>
              </Box>

              <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                <FormControlLabel
                  control={
                    <Switch checked={privacy.publicProfile} onChange={handlePrivacyChange} name="publicProfile" />
                  }
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Public Profile
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Make your profile visible to others
                      </Typography>
                    </Box>
                  }
                />

                <Divider />

                <FormControlLabel
                  control={<Switch checked={privacy.showEmail} onChange={handlePrivacyChange} name="showEmail" />}
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Show Email Address
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Display your email on your public profile
                      </Typography>
                    </Box>
                  }
                />

                <Divider />

                <FormControlLabel
                  control={
                    <Switch checked={privacy.dataCollection} onChange={handlePrivacyChange} name="dataCollection" />
                  }
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        Analytics & Data Collection
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Help us improve by sharing anonymous usage data
                      </Typography>
                    </Box>
                  }
                />
              </Box>

              <Divider sx={{ my: 3 }} />

              <Box>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 1 }}>
                  Password
                </Typography>
                <Button variant="outlined" size="small">
                  Change Password
                </Button>
              </Box>

              <Divider sx={{ my: 3 }} />

              <Box>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 1, color: "error.main" }}>
                  Danger Zone
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 1.5 }}>
                  Once you delete your account, there is no going back. Please be certain.
                </Typography>
                <Button variant="outlined" color="error" size="small">
                  Delete Account
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Payment Method */}
        <Grid item xs={12}>
          <Card>
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

      {/* Save Button */}
      <Box sx={{ mt: 4, display: "flex", justifyContent: "flex-end", gap: 2 }}>
        <Button variant="outlined" onClick={onBack}>
          Cancel
        </Button>
        <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave}>
          Save Changes
        </Button>
      </Box>
    </Box>
  );
}
