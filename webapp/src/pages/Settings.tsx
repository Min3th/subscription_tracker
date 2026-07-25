import { useState, useEffect } from "react";
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
  Avatar,
  IconButton,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  FormControlLabel,
  Radio,
  RadioGroup,
} from "@mui/material";
import PersonIcon from "@mui/icons-material/Person";
import NotificationsIcon from "@mui/icons-material/Notifications";
import PublicIcon from "@mui/icons-material/Public";
import PaletteIcon from "@mui/icons-material/Palette";
import PhotoCameraIcon from "@mui/icons-material/PhotoCamera";
import SaveIcon from "@mui/icons-material/Save";
import type { SelectChangeEvent } from "@mui/material";
import { useSelector, useDispatch } from "react-redux";
import type { RootState, AppDispatch } from "../app/store";
import { fetchPreferences, updatePreferences, setPreferences } from "../features/preferences/preferencesSlice";
import { useTranslation } from "react-i18next";
import { useBlocker } from "react-router-dom";
import { useSnackbar } from "../utils/Snackbar";

export function Settings() {
  const { t } = useTranslation();
  const { user } = useSelector((state: RootState) => state.auth);
  const preferences = useSelector((state: RootState) => state.preferences);
  const dispatch = useDispatch<AppDispatch>();
  const snackbar = useSnackbar();

  const [initialPreferences, setInitialPreferences] = useState<{
    currency: string;
    language: string;
    timezone: string;
    theme: string;
    emailNotificationsEnabled: boolean;
    reminderDaysBefore: number;
  } | null>(null);

  const [formData, setFormData] = useState({
    name: user?.name || "",
    email: user?.email || "",
    currency: "USD",
    timezone: "America/New_York",
    theme: "light",
    language: "en",
    emailNotificationsEnabled: true,
    reminderDaysBefore: 3,
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
        emailNotificationsEnabled: preferences.emailNotificationsEnabled,
        reminderDaysBefore: preferences.reminderDaysBefore,
      }));
      if (!initialPreferences) {
        setInitialPreferences({
          currency: preferences.currency,
          language: preferences.language,
          timezone: preferences.timezone,
          theme: preferences.theme,
          emailNotificationsEnabled: preferences.emailNotificationsEnabled,
          reminderDaysBefore: preferences.reminderDaysBefore,
        });
      }
    }
  }, [preferences.status]);

  const [openDialog, setOpenDialog] = useState(false);

  const isDirty =
    formData.name !== (user?.name || "") ||
    formData.email !== (user?.email || "") ||
    (initialPreferences &&
      (formData.currency !== initialPreferences.currency ||
        formData.timezone !== initialPreferences.timezone ||
        formData.theme !== initialPreferences.theme ||
        formData.language !== initialPreferences.language ||
        formData.emailNotificationsEnabled !== initialPreferences.emailNotificationsEnabled ||
        formData.reminderDaysBefore !== initialPreferences.reminderDaysBefore));

  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    Boolean(isDirty && currentLocation.pathname !== nextLocation.pathname),
  );

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
      dispatch(setPreferences({ theme: value }));
    }
  };

  const handleSave = () => {
    setOpenDialog(true);
  };

  const handleConfirmSave = async () => {
    try {
      await dispatch(updatePreferences(formData)).unwrap();
      setInitialPreferences({
        currency: formData.currency,
        language: formData.language,
        timezone: formData.timezone,
        theme: formData.theme,
        emailNotificationsEnabled: formData.emailNotificationsEnabled,
        reminderDaysBefore: formData.reminderDaysBefore,
      });
      snackbar.success(t("settings.saved_success"));
    } catch (err) {
      console.error("Failed to update preferences:", err);
      snackbar.error(t("settings.save_error", "Failed to update preferences"));
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

  const handleReminderDaysChange = (days: number) => {
    setFormData({
      ...formData,
      reminderDaysBefore: days,
    });
  };

  const handleEmailNotificationsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      emailNotificationsEnabled: e.target.checked,
    });
  };

  return (
    <Box
      display="flex"
      flexDirection="column"
      gap={3}
      sx={{
        width: "100%",
        minWidth: 0,
        maxWidth: 1000,
        boxSizing: "border-box",
        mx: "auto",
        p: { xs: 0, md: 3 },
      }}
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

      <Grid container spacing={3}>
        <Grid size={{ xs: 12 }}>
          <Card elevation={0} sx={{ bgcolor: "transparent", backgroundImage: "none" }}>
            <CardContent sx={{ p: { xs: 0, md: 3 } }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
                <PersonIcon sx={{ color: "primary.main" }} />
                <Typography variant="h6" sx={{ fontWeight: 600 }}>
                  {t("settings.profile_info")}
                </Typography>
              </Box>

              <Box
                sx={{
                  display: "flex",
                  flexDirection: { xs: "column", md: "row" },
                  alignItems: { xs: "stretch", md: "center" },
                  gap: 3,
                  mb: 3,
                }}
              >
                <Box sx={{ position: "relative", alignSelf: { xs: "center", md: "auto" } }}>
                  <Avatar
                    src={user.picture}
                    alt={user.name}
                    sx={{ width: 100, height: 100 }}
                    imgProps={{ referrerPolicy: "no-referrer" }}
                  />
                  <IconButton
                    sx={{
                      position: "absolute",
                      bottom: -5,
                      right: -5,
                      bgcolor: "primary.main",
                      color: "white",
                      width: { xs: 44, md: 36 },
                      height: { xs: 44, md: 36 },
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
                <Grid container spacing={2} sx={{ width: "100%", minWidth: 0, flex: 1 }}>
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
            <CardContent sx={{ p: { xs: 0, md: 3 } }}>
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
            <CardContent sx={{ p: { xs: 0, md: 3 } }}>
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
            <CardContent sx={{ p: { xs: 0, md: 3 } }}>
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
                      Renewal reminder emails
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Receive an email before recurring subscriptions renew
                    </Typography>
                  </Box>
                  <Switch
                    checked={formData.emailNotificationsEnabled}
                    onChange={handleEmailNotificationsChange}
                    name="emailNotificationsEnabled"
                  />
                </Box>
                <Box sx={{ textAlign: "left", opacity: formData.emailNotificationsEnabled ? 1 : 0.5 }}>
                  <Typography variant="body2" sx={{ fontWeight: 500, mb: 1 }}>
                    Remind me:
                  </Typography>

                  <RadioGroup
                    value={formData.reminderDaysBefore}
                    onChange={(e) => handleReminderDaysChange(Number(e.target.value))}
                    sx={{ display: "flex", flexDirection: "column", gap: 1 }}
                  >
                    <FormControlLabel
                      value={3}
                      control={<Radio disabled={!formData.emailNotificationsEnabled} />}
                      label="3 days before"
                    />
                    <FormControlLabel
                      value={1}
                      control={<Radio disabled={!formData.emailNotificationsEnabled} />}
                      label="1 day before"
                    />
                    <FormControlLabel
                      value={0}
                      control={<Radio disabled={!formData.emailNotificationsEnabled} />}
                      label="On billing day"
                    />
                  </RadioGroup>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      <Box sx={{ mt: 4, display: "flex", justifyContent: "flex-end", gap: 2 }}>
        <Button
          variant="contained"
          startIcon={<SaveIcon />}
          onClick={handleSave}
          sx={{ width: { xs: "100%", sm: "auto" }, minHeight: { xs: 44, sm: "auto" } }}
        >
          {t("settings.save")}
        </Button>
      </Box>

      {/* Confirmation Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog}>
        <DialogTitle>{t("settings.confirm_title")}</DialogTitle>
        <DialogContent>
          <DialogContentText>{t("settings.confirm_desc")}</DialogContentText>
        </DialogContent>
        <DialogActions
          sx={{
            flexDirection: { xs: "column", sm: "row" },
            alignItems: "stretch",
            gap: { xs: 1, sm: 0 },
            "& .MuiButton-root": {
              width: { xs: "100%", sm: "auto" },
              minHeight: { xs: 44, sm: "auto" },
              m: { xs: "0 !important", sm: undefined },
            },
          }}
        >
          <Button onClick={handleCloseDialog} color="inherit">
            {t("settings.cancel")}
          </Button>
          <Button onClick={handleConfirmSave} variant="contained" color="primary">
            {t("settings.confirm_button")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Unsaved Changes Navigation Blocker Dialog */}
      <Dialog open={blocker.state === "blocked"} onClose={() => blocker.state === "blocked" && blocker.reset()}>
        <DialogTitle>{t("settings.unsaved_changes_title", "Unsaved Changes")}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t("settings.unsaved_changes_desc", "You have unsaved changes. Do you want to confirm the changes or not?")}
          </DialogContentText>
        </DialogContent>
        <DialogActions
          sx={{
            flexDirection: { xs: "column", sm: "row" },
            alignItems: "stretch",
            gap: { xs: 1, sm: 0 },
            "& .MuiButton-root": {
              width: { xs: "100%", sm: "auto" },
              minHeight: { xs: 44, sm: "auto" },
              m: { xs: "0 !important", sm: undefined },
            },
          }}
        >
          <Button onClick={() => blocker.state === "blocked" && blocker.reset()} color="inherit">
            {t("settings.cancel", "Cancel")}
          </Button>
          <Button
            onClick={() => {
              if (blocker.state === "blocked") {
                dispatch(fetchPreferences());
                blocker.proceed();
              }
            }}
            color="error"
          >
            {t("settings.discard", "Discard")}
          </Button>
          <Button
            onClick={async () => {
              if (blocker.state === "blocked") {
                await handleConfirmSave();
                blocker.proceed();
              }
            }}
            variant="contained"
            color="primary"
          >
            {t("settings.save_and_continue", "Save & Continue")}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
