import { useState } from "react";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  IconButton,
  LinearProgress,
  MobileStepper,
  Stack,
  Switch,
  Typography,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import WalletIcon from "@mui/icons-material/Wallet";
import AddCardIcon from "@mui/icons-material/AddCard";
import InsightsIcon from "@mui/icons-material/Insights";
import NotificationsActiveIcon from "@mui/icons-material/NotificationsActive";
import KeyboardArrowLeftIcon from "@mui/icons-material/KeyboardArrowLeft";
import KeyboardArrowRightIcon from "@mui/icons-material/KeyboardArrowRight";
import { useTranslation } from "react-i18next";

type CompletionAction = "dismiss" | "add-subscription";

type Props = {
  open: boolean;
  onComplete: (action: CompletionAction) => void | Promise<void>;
};

const slideIcons = [WalletIcon, AddCardIcon, InsightsIcon, NotificationsActiveIcon];

function ProductPreview({ step }: { step: number }) {
  const panel = {
    bgcolor: "background.paper",
    border: 1,
    borderColor: "divider",
    borderRadius: 1.5,
  };

  return (
    <Box
      aria-hidden="true"
      sx={{
        width: "100%",
        maxWidth: 500,
        height: { xs: 190, sm: 220 },
        mb: 3,
        p: { xs: 1.5, sm: 2 },
        overflow: "hidden",
        borderRadius: 2.5,
        border: 1,
        borderColor: "divider",
        bgcolor: "action.hover",
        boxShadow: "inset 0 1px 0 rgba(255,255,255,0.08)",
        pointerEvents: "none",
      }}
    >
      {step === 0 && (
        <Stack direction="row" gap={1.5} height="100%">
          <Stack sx={{ ...panel, width: 86, p: 1.25 }} gap={1}>
            <Box sx={{ width: 24, height: 24, borderRadius: 1, bgcolor: "primary.main" }} />
            {[50, 42, 46].map((width) => (
              <Box key={width} sx={{ width, height: 6, borderRadius: 4, bgcolor: "text.disabled" }} />
            ))}
          </Stack>
          <Stack flex={1} gap={1.25}>
            <Stack direction="row" gap={1}>
              {[
                ["Monthly Spent", "$84"],
                ["Yearly Projected", "$1,008"],
                ["Active Subscriptions", "4"],
              ].map(([label, value]) => (
                <Stack key={label} sx={{ ...panel, flex: 1, minWidth: 0, p: 1 }} gap={0.5}>
                  <Typography fontSize={{ xs: 7, sm: 9 }} color="text.secondary" noWrap title={label}>
                    {label}
                  </Typography>
                  <Typography fontSize={{ xs: 13, sm: 17 }} fontWeight={800}>
                    {value}
                  </Typography>
                </Stack>
              ))}
            </Stack>
            <Box sx={{ ...panel, flex: 1, p: 1.25 }}>
              <Stack direction="row" alignItems="flex-end" gap={1} height="100%">
                {[34, 58, 45, 72, 52, 84].map((height, index) => (
                  <Box
                    key={index}
                    sx={{ flex: 1, height: `${height}%`, borderRadius: "4px 4px 0 0", bgcolor: "primary.main" }}
                  />
                ))}
              </Stack>
            </Box>
          </Stack>
        </Stack>
      )}

      {step === 1 && (
        <Stack sx={{ ...panel, maxWidth: 400, height: "100%", mx: "auto", p: { xs: 1.5, sm: 2 } }} gap={1.25}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography fontSize={{ xs: 13, sm: 16 }} fontWeight={700}>
              Add Subscription
            </Typography>
            <Typography fontSize={10} color="primary.main">
              Basic information · Billing
            </Typography>
          </Stack>
          <Stack direction="row" gap={1}>
            <Box sx={{ ...panel, flex: 2, height: 37, px: 1.25, display: "flex", alignItems: "center" }}>
              <Typography fontSize={11} color="text.secondary">
                Streaming service
              </Typography>
            </Box>
            <Box sx={{ ...panel, flex: 1, height: 37, px: 1.25, display: "flex", alignItems: "center" }}>
              <Typography fontSize={11} color="text.secondary">
                Monthly
              </Typography>
            </Box>
          </Stack>
          <Stack direction="row" gap={1}>
            <Box sx={{ ...panel, flex: 1, height: 37, px: 1.25, display: "flex", alignItems: "center" }}>
              <Typography fontSize={11} color="text.secondary">
                USD 12.99
              </Typography>
            </Box>
            <Box sx={{ ...panel, flex: 1, height: 37, px: 1.25, display: "flex", alignItems: "center" }}>
              <Typography fontSize={11} color="text.secondary">
                Entertainment
              </Typography>
            </Box>
          </Stack>
          <Box
            sx={{
              alignSelf: "flex-end",
              mt: "auto",
              width: 72,
              height: 28,
              borderRadius: 1,
              bgcolor: "primary.main",
              color: "primary.contrastText",
              display: "grid",
              placeItems: "center",
              fontSize: 10,
              fontWeight: 700,
              letterSpacing: 0.4,
            }}
          >
            NEXT
          </Box>
        </Stack>
      )}

      {step === 2 && (
        <Stack direction={{ xs: "column", sm: "row" }} gap={1.25} height="100%">
          <Stack sx={{ ...panel, flex: 1.5, p: 1.5 }} gap={1}>
            <Typography fontSize={12} fontWeight={700}>
              Monthly spending
            </Typography>
            <Stack direction="row" alignItems="flex-end" gap={1} flex={1}>
              {[40, 54, 48, 76, 64, 88].map((height, index) => (
                <Stack key={index} flex={1} height="100%" justifyContent="flex-end" alignItems="center" gap={0.5}>
                  <Box
                    sx={{ width: "100%", height: `${height}%`, borderRadius: "4px 4px 0 0", bgcolor: "primary.main" }}
                  />
                  <Typography fontSize={8} color="text.secondary">
                    {["Feb", "Mar", "Apr", "May", "Jun", "Jul"][index]}
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </Stack>
          <Stack sx={{ ...panel, flex: 1, p: 1.5 }} gap={1.25}>
            <Typography fontSize={12} fontWeight={700}>
              By category
            </Typography>
            {[
              ["Entertainment", 72],
              ["Software", 52],
              ["Music", 34],
            ].map(([label, value]) => (
              <Box key={label}>
                <Typography fontSize={9} color="text.secondary">
                  {label}
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={Number(value)}
                  sx={{ mt: 0.5, height: 7, borderRadius: 4 }}
                />
              </Box>
            ))}
          </Stack>
        </Stack>
      )}

      {step === 3 && (
        <Stack sx={{ ...panel, maxWidth: 430, height: "100%", mx: "auto", p: { xs: 1.5, sm: 2 } }} gap={1.25}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Box>
              <Typography fontSize={{ xs: 12, sm: 14 }} fontWeight={700}>
                Renewal reminder emails
              </Typography>
              <Typography fontSize={9} color="text.secondary">
                Get notified before a subscription renews
              </Typography>
            </Box>
            <Switch size="small" defaultChecked />
          </Stack>
          {["3 days before", "1 day before", "On billing day"].map((label, index) => (
            <Stack key={label} direction="row" alignItems="center" gap={1} sx={{ ...panel, px: 1.25, py: 0.75 }}>
              <Box
                sx={{
                  width: 14,
                  height: 14,
                  borderRadius: "50%",
                  border: 2,
                  borderColor: "primary.main",
                  bgcolor: index === 0 ? "primary.main" : "transparent",
                }}
              />
              <Typography fontSize={11}>{label}</Typography>
            </Stack>
          ))}
        </Stack>
      )}
    </Box>
  );
}

export default function WelcomeGuide({ open, onComplete }: Props) {
  const { t } = useTranslation();
  const [activeStep, setActiveStep] = useState(0);
  const [completing, setCompleting] = useState(false);
  const lastStep = slideIcons.length - 1;
  const SlideIcon = slideIcons[activeStep];

  const complete = async (action: CompletionAction) => {
    if (completing) return;
    setCompleting(true);
    try {
      await onComplete(action);
    } finally {
      setCompleting(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={() => complete("dismiss")}
      fullWidth
      maxWidth="md"
      aria-labelledby="welcome-guide-title"
      slotProps={{
        paper: {
          sx: {
            m: { xs: 2, sm: 4 },
            width: { xs: "calc(100% - 32px)", sm: "calc(100% - 64px)" },
            height: { xs: "min(680px, calc(100dvh - 32px))", sm: 650 },
            maxHeight: { xs: "calc(100dvh - 32px)", sm: "calc(100dvh - 64px)" },
            borderRadius: 3,
            overflow: "hidden",
          },
        },
      }}
    >
      <DialogContent
        sx={{
          position: "relative",
          flex: "1 1 auto",
          minHeight: 0,
          overflowY: "auto",
          px: { xs: 2.5, sm: 6 },
          pt: { xs: 5, sm: 5 },
          pb: 2,
        }}
      >
        <IconButton
          aria-label={t("onboarding.close")}
          onClick={() => complete("dismiss")}
          disabled={completing}
          sx={{ position: "absolute", right: 12, top: 12 }}
        >
          <CloseIcon />
        </IconButton>

        <Stack alignItems="center" textAlign="center">
          <Box
            aria-hidden="true"
            sx={{
              width: 48,
              height: 48,
              borderRadius: "50%",
              display: "grid",
              placeItems: "center",
              bgcolor: "primary.main",
              color: "primary.contrastText",
              mb: 1.5,
            }}
          >
            <SlideIcon sx={{ fontSize: 25 }} />
          </Box>
          <Typography
            id="welcome-guide-title"
            variant="h4"
            fontWeight={800}
            gutterBottom
            sx={{ minHeight: { xs: 42, sm: 46 }, display: "flex", alignItems: "center" }}
          >
            {t(`onboarding.slides.${activeStep}.title`)}
          </Typography>
          <Typography
            color="text.secondary"
            sx={{
              maxWidth: 600,
              minHeight: { xs: 77, sm: 52 },
              lineHeight: 1.6,
              mb: 2.5,
              display: "flex",
              alignItems: "flex-start",
              justifyContent: "center",
            }}
          >
            {t(`onboarding.slides.${activeStep}.body`)}
          </Typography>
          <ProductPreview step={activeStep} />
        </Stack>
      </DialogContent>

      <MobileStepper
        variant="dots"
        steps={slideIcons.length}
        position="static"
        activeStep={activeStep}
        backButton={<Box />}
        nextButton={<Box />}
        sx={{ justifyContent: "center", flex: "0 0 auto", bgcolor: "transparent", py: 1 }}
      />

      <DialogActions
        sx={{
          px: { xs: 3, sm: 4 },
          pb: 3,
          pt: 1,
          justifyContent: "space-between",
          flexWrap: "nowrap",
          gap: 1,
          height: 76,
          flex: "0 0 76px",
          "& .MuiButton-root": {
            whiteSpace: "nowrap",
            minWidth: { xs: 40, sm: 64 },
            px: { xs: 1, sm: 2 },
            fontSize: { xs: "0.7rem", sm: "0.875rem" },
          },
        }}
      >
        {activeStep === 0 ? (
          <Button variant="outlined" color="inherit" onClick={() => complete("dismiss")} disabled={completing}>
            {t("onboarding.skip")}
          </Button>
        ) : (
          <Button
            variant="outlined"
            color="inherit"
            startIcon={<KeyboardArrowLeftIcon />}
            onClick={() => setActiveStep((step) => step - 1)}
            disabled={completing}
          >
            {t("onboarding.back")}
          </Button>
        )}

        {activeStep < lastStep ? (
          <Button
            variant="contained"
            endIcon={<KeyboardArrowRightIcon />}
            onClick={() => setActiveStep((step) => step + 1)}
          >
            {t("onboarding.next")}
          </Button>
        ) : (
          <Stack direction="row" gap={1} minWidth={0}>
            <Button
              variant="outlined"
              color="inherit"
              onClick={() => complete("dismiss")}
              disabled={completing}
              sx={{ flexShrink: 1 }}
            >
              {t("onboarding.maybe_later")}
            </Button>
            <Button
              variant="contained"
              onClick={() => complete("add-subscription")}
              disabled={completing}
              sx={{ flexShrink: 1 }}
            >
              {t("onboarding.add_first")}
            </Button>
          </Stack>
        )}
      </DialogActions>
    </Dialog>
  );
}
