import { useState, useEffect, useContext } from "react";
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
  Divider,
  Avatar,
  IconButton,
  Alert,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Snackbar,
} from "@mui/material";
import PersonIcon from "@mui/icons-material/Person";
import NotificationsIcon from "@mui/icons-material/Notifications";
import PublicIcon from "@mui/icons-material/Public";
import PaletteIcon from "@mui/icons-material/Palette";
import CreditCardIcon from "@mui/icons-material/CreditCard";
import PhotoCameraIcon from "@mui/icons-material/PhotoCamera";
import SaveIcon from "@mui/icons-material/Save";
import type { SelectChangeEvent } from "@mui/material";
import { useSelector, useDispatch } from "react-redux";
import type { RootState, AppDispatch } from "../app/store";
import { fetchPreferences, updatePreferences, setPreferences } from "../features/preferences/preferencesSlice";
import { ColorModeContext } from "../theme/ThemeContext";
import { useTranslation } from "react-i18next";

export function Settings() {
  const { t } = useTranslation();
  const { user } = useSelector((state: RootState) => state.auth);
  const preferences = useSelector((state: RootState) => state.preferences);
  const [localTheme, setLocalTheme] = useState(preferences.theme);
  const { setMode } = useContext(ColorModeContext);
  const dispatch = useDispatch<AppDispatch>();

  const [formData, setFormData] = useState({
    name: user?.name || "",
    email: user?.email || "",
    currency: "USD",
    timezone: "America/New_York",
    theme: "light",
    language: "en",
  });

  useEffect(() => {
    if (user) {
      setFormData((prev) => ({
        ...prev,
        name: user.name,
        email: user.email,
      }));
    }
  }, [user?.name, user?.email]);

  useEffect(() => {
    dispatch(fetchPreferences());
  }, [dispatch]);

  useEffect(() => {
    if (preferences.status === "succeeded") {
      setFormData((prev) => ({
        ...prev,
        currency: preferences.currency,
        language: preferences.language,
        timezone: preferences.timezone,
        theme: preferences.theme,
      }));
    }
  }, [preferences.status, preferences.currency, preferences.language, preferences.timezone, preferences.theme]);

  const [notifications, setNotifications] = useState({
    emailNotifications: true,
    upcomingBilling: true,
    weeklyReport: false,
    renewalReminders: true,
    priceChanges: true,
  });

  const [saved, setSaved] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);

  if (!user) {
    return <Typography>Loading...</Typography>;
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSelectChange = (e: SelectChangeEvent) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value,
    });

    if (name === "theme") {
      setLocalTheme(value);
      dispatch(setPreferences({ theme: value }));
    }
  };

  const handleNotificationChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNotifications({
      ...notifications,
      [e.target.name]: e.target.checked,
    });
  };

  const handleSave = () => {
    setOpenDialog(true);
  };

  const handleCancel = () => {
    dispatch(fetchPreferences());
  };

  const handleConfirmSave = async () => {
    try {
      await dispatch(updatePreferences(formData)).unwrap();
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (err) {
      console.error("Failed to update preferences:", err);
    } finally {
      setOpenDialog(false);
    }
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
  };

  const currencies = [
    { code: "USD", symbol: "$", name: "US Dollar" },
    { code: "EUR", symbol: "€", name: "Euro" },
    { code: "GBP", symbol: "£", name: "British Pound" },
    { code: "JPY", symbol: "¥", name: "Japanese Yen" },
    { code: "CAD", symbol: "C$", name: "Canadian Dollar" },
    { code: "AUD", symbol: "A$", name: "Australian Dollar" },
    { code: "INR", symbol: "₹", name: "Indian Rupee" },
    { code: "LKR", symbol: "Rs", name: "Sri Lankan Rupee" },
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
    "Asia/Colombo",
    "Australia/Sydney",
  ];

  const themes = [
    { value: "light", label: "Light" },
    { value: "dark", label: "Dark" },
    { value: "auto", label: "Auto (System)" },
  ];

  const languages = [
    { code: "en", name: "English" },
    { code: "si", name: "Sinhala" },
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
          {t("settings.title")}
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5 }}>
          {t("settings.subtitle")}
        </Typography>
      </Box>

      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {t("settings.saved_success")}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <PersonIcon sx={{ color: "primary.main" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  {t("settings.profile_info")}
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
                    {t("settings.profile_photo")}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {t("settings.profile_photo_desc")}
                  </Typography>
                </Box>
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label={t("settings.full_name")}
                      name="name"
                      value={formData.name}
                      onChange={handleInputChange}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label={t("settings.email")}
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
                <PublicIcon sx={{ color: "primary.main" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  {t("settings.preferences")}
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
                    <InputLabel>{t("settings.language", "Language")}</InputLabel>
                    <Select
                      name="language"
                      value={formData.language}
                      onChange={handleSelectChange}
                      label={t("settings.language", "Language")}
                    >
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
                    <InputLabel>{t("settings.timezone", "Timezone")}</InputLabel>
                    <Select
                      name="timezone"
                      value={formData.timezone}
                      onChange={handleSelectChange}
                      label={t("settings.timezone", "Timezone")}
                    >
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
        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <PaletteIcon sx={{ color: "primary.main" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  {t("settings.appearance")}
                </Typography>
              </Box>

              <FormControl fullWidth>
                <InputLabel>{t("settings.theme", "Theme")}</InputLabel>
                <Select
                  name="theme"
                  value={formData.theme}
                  onChange={handleSelectChange}
                  label={t("settings.theme", "Theme")}
                >
                  {themes.map((theme) => (
                    <MenuItem key={theme.value} value={theme.value}>
                      {theme.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: "block" }}>
                {t("settings.theme_desc")}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <NotificationsIcon sx={{ color: "primary.main" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  {t("settings.notifications")}
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
                      {t("settings.upcoming_alerts")}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {t("settings.upcoming_alerts_desc")}
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
                      {t("settings.renewal_reminders")}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {t("settings.renewal_reminders_desc")}
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
                      {t("settings.price_change")}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {t("settings.price_change_desc")}
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
                      {t("settings.weekly_report")}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {t("settings.weekly_report_desc")}
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
                <CreditCardIcon sx={{ color: "primary.main" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  {t("settings.payment_methods")}
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
                        bgcolor: "action.hover",
                        borderRadius: 0.5,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                      }}
                    >
                      <CreditCardIcon sx={{ color: "primary.main" }} />
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
                    {t("settings.edit")}
                  </Button>
                </Box>

                <Button variant="outlined" fullWidth>
                  {t("settings.add_payment")}
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      <Box sx={{ mt: 4, display: "flex", justifyContent: "flex-end", gap: 2 }}>
        <Button variant="outlined" onClick={handleCancel}>
          {t("settings.cancel")}
        </Button>
        <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave}>
          {t("settings.save")}
        </Button>
      </Box>

      {/* Confirmation Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog}>
        <DialogTitle>{t("settings.confirm_title")}</DialogTitle>
        <DialogContent>
          <DialogContentText>{t("settings.confirm_desc")}</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} color="inherit">
            {t("settings.cancel")}
          </Button>
          <Button onClick={handleConfirmSave} variant="contained" color="primary">
            {t("settings.confirm_button")}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
