import { useState } from "react";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  IconButton,
  MobileStepper,
  Stack,
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
      maxWidth="sm"
      aria-labelledby="welcome-guide-title"
      slotProps={{
        paper: {
          sx: {
            m: { xs: 2, sm: 4 },
            width: { xs: "calc(100% - 32px)", sm: "calc(100% - 64px)" },
            borderRadius: 3,
          },
        },
      }}
    >
      <DialogContent sx={{ position: "relative", px: { xs: 3, sm: 6 }, pt: { xs: 6, sm: 7 }, pb: 3 }}>
        <IconButton
          aria-label={t("onboarding.close")}
          onClick={() => complete("dismiss")}
          disabled={completing}
          sx={{ position: "absolute", right: 12, top: 12 }}
        >
          <CloseIcon />
        </IconButton>

        <Stack alignItems="center" textAlign="center" minHeight={{ xs: 300, sm: 320 }}>
          <Box
            aria-hidden="true"
            sx={{
              width: 88,
              height: 88,
              borderRadius: "50%",
              display: "grid",
              placeItems: "center",
              bgcolor: "primary.main",
              color: "primary.contrastText",
              mb: 4,
            }}
          >
            <SlideIcon sx={{ fontSize: 44 }} />
          </Box>
          <Typography id="welcome-guide-title" variant="h4" fontWeight={800} gutterBottom>
            {t(`onboarding.slides.${activeStep}.title`)}
          </Typography>
          <Typography color="text.secondary" sx={{ maxWidth: 430, lineHeight: 1.7 }}>
            {t(`onboarding.slides.${activeStep}.body`)}
          </Typography>
        </Stack>
      </DialogContent>

      <MobileStepper
        variant="dots"
        steps={slideIcons.length}
        position="static"
        activeStep={activeStep}
        backButton={<Box />}
        nextButton={<Box />}
        sx={{ justifyContent: "center", bgcolor: "transparent", py: 1 }}
      />

      <DialogActions
        sx={{
          px: { xs: 3, sm: 4 },
          pb: 3,
          pt: 1,
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: 1,
        }}
      >
        {activeStep === 0 ? (
          <Button color="inherit" onClick={() => complete("dismiss")} disabled={completing}>
            {t("onboarding.skip")}
          </Button>
        ) : (
          <Button
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
          <Stack direction={{ xs: "column-reverse", sm: "row" }} gap={1} width={{ xs: "100%", sm: "auto" }}>
            <Button color="inherit" onClick={() => complete("dismiss")} disabled={completing}>
              {t("onboarding.maybe_later")}
            </Button>
            <Button variant="contained" onClick={() => complete("add-subscription")} disabled={completing}>
              {t("onboarding.add_first")}
            </Button>
          </Stack>
        )}
      </DialogActions>
    </Dialog>
  );
}
