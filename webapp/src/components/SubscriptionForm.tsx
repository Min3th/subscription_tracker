import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  MenuItem,
  Box,
  Typography,
  Stepper,
  Step,
  StepLabel,
  FormControlLabel,
  Switch,
} from "@mui/material";
import { useState } from "react";
import * as Yup from "yup";
import { useFormik } from "formik";
import { useSnackbar } from "../utils/Snackbar";
import { useEffect } from "react";
import CircularProgress from "@mui/material/CircularProgress";
import { useTheme } from "@mui/material";
import { updateSubscriptionThunk, createSubscriptionThunk, fetchSubscriptionById } from "../app/subscriptionSlice";
import { useDispatch, useSelector } from "react-redux";
import type { AppDispatch, RootState } from "../app/store";
import {
  SUBSCRIPTION_CATEGORIES,
  type UpdateSubscriptionPayload,
  type SubscriptionType,
  type BillingUnit,
  type SubscriptionCategory,
} from "../types/subscription";
import { parseDecimal, SUPPORTED_CURRENCIES } from "../utils/money";

type Props = {
  open: boolean;
  handleClose: () => void;
  onSuccess?: () => void;
  editId?: number | null;
};

type FormValues = {
  name: string;
  description: string;
  cost: string;
  currency: string;
  type: SubscriptionType;
  category: SubscriptionCategory | "";
  startDate: string;
  paymentMethod: string;
  website: string;
  billingIntervalUnit: BillingUnit;
  billingIntervalCount: number;
  emailNotificationsEnabled: boolean;
};

export default function SubscriptionForm({ open, handleClose, onSuccess, editId }: Props) {
  const [loading, setLoading] = useState(false);
  const [activeStep, setActiveStep] = useState(0);
  const snackbar = useSnackbar();
  const theme = useTheme();
  const dispatch = useDispatch<AppDispatch>();
  const preferredCurrency = useSelector((state: RootState) => state.preferences.currency || "USD");

  const validationSchema = Yup.object({
    name: Yup.string().required("Name is required"),
    description: Yup.string(),
    cost: Yup.string()
      .matches(/^\d+(\.\d{1,4})?$/, "Use a positive amount with at most 4 decimal places")
      .test("positive", "Must be positive", (value) => {
        try {
          return Boolean(value && parseDecimal(value) > 0n);
        } catch {
          return false;
        }
      })
      .required("Amount is required"),
    currency: Yup.string().oneOf([...SUPPORTED_CURRENCIES]).required("Currency is required"),
    type: Yup.string().required("Type is required"),
    category: Yup.string().required("Category is required"),
    startDate: Yup.date().required("Date is required"),
    billingIntervalUnit: Yup.string().required("Required"),
    billingIntervalCount: Yup.number().positive("Must be positive").required("Required"),
  });

  const formik = useFormik<FormValues>({
    initialValues: {
      name: "",
      description: "",
      cost: "",
      currency: preferredCurrency,
      type: "recurring",
      category: "",
      startDate: "",
      paymentMethod: "",
      website: "",
      billingIntervalUnit: "month",
      billingIntervalCount: 1,
      emailNotificationsEnabled: false,
    },
    validationSchema,
    onSubmit: async (values) => {
      try {
        const payload = {
          id: editId,
          name: values.name,
          description: values.description,
          cost: values.cost,
          currency: values.currency,
          type: values.type,
          category: values.category as SubscriptionCategory,
          startDate: values.startDate,
          paymentMethod: values.paymentMethod,
          website: values.website,
          billingIntervalUnit: values.billingIntervalUnit,
          billingIntervalCount: Number(values.billingIntervalCount),
          emailNotificationsEnabled: Boolean(values.emailNotificationsEnabled),
        };

        if (editId) {
          const updatePayload: UpdateSubscriptionPayload = {
            id: editId,
            name: values.name,
            description: values.description,
            cost: values.cost,
            currency: values.currency,
            type: values.type,
            category: values.category as SubscriptionCategory,
            startDate: values.startDate,
            paymentMethod: values.paymentMethod,
            website: values.website,
            billingIntervalUnit: values.billingIntervalUnit,
            billingIntervalCount: Number(values.billingIntervalCount),
            emailNotificationsEnabled: Boolean(values.emailNotificationsEnabled),
          };
          await dispatch(updateSubscriptionThunk(updatePayload));
          snackbar.success("Subscription updated successfully!");
        } else {
          await dispatch(createSubscriptionThunk(payload));
          snackbar.success("Subscription created successfully!");
        }

        handleClose();
        if (onSuccess) {
          onSuccess();
        }
      } catch (error) {
        console.error("Error saving subscription:", error);
        snackbar.error("Failed to save subscription. Please try again.");
      }
    },
  });

  useEffect(() => {
    if (open) setActiveStep(0);

    if (open && editId) {
      setLoading(true);
      dispatch(fetchSubscriptionById(editId))
        .unwrap()
        .then((data) => {
          formik.setValues({
            name: data.name || "",
            description: data.description || "",
            cost: data.cost || "",
            currency: data.currency || preferredCurrency,
            type: data.type || "recurring",
            category: data.category || "",
            startDate: data.startDate || "",
            paymentMethod: data.paymentMethod || "",
            website: data.website || "",
            billingIntervalUnit: data.billingIntervalUnit || "month",
            billingIntervalCount: data.billingIntervalCount || 1,
            emailNotificationsEnabled: data.emailNotificationsEnabled || false,
          });
        })
        .catch((err: any) => {
          console.error("Error fetching subscription:", err);
          snackbar.error("Failed to fetch subscription details.");
        })
        .finally(() => {
          setLoading(false);
        });
    } else if (open && !editId) {
      formik.resetForm({ values: { ...formik.initialValues, currency: preferredCurrency } });
    }
  }, [open, editId]);

  const handleNext = async () => {
    const errors = await formik.validateForm();
    let hasError = false;

    const step1Fields = ["name", "type", "category"];
    if (formik.values.type === "recurring") {
      step1Fields.push("billingIntervalUnit", "billingIntervalCount");
    }

    step1Fields.forEach((field) => {
      if (errors[field as keyof typeof errors]) {
        formik.setFieldTouched(field, true, false);
        hasError = true;
      }
    });

    if (!hasError) {
      setActiveStep((prev) => prev + 1);
    }
  };

  const handleBack = () => {
    setActiveStep((prev) => prev - 1);
  };

  const handleDialogClose = () => {
    formik.resetForm();
    setActiveStep(0);
    handleClose();
  };

  const steps = ["Basic Information", "Billing Details"];

  return (
    <>
      <Dialog open={open} onClose={handleDialogClose} fullWidth maxWidth="sm">
        <DialogTitle>{editId ? "Edit Subscription" : "Add Subscription"}</DialogTitle>

        <DialogContent>
          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress />
            </Box>
          ) : (
            <>
              <Stepper activeStep={activeStep} sx={{ pt: 2, pb: 4 }}>
                {steps.map((label) => (
                  <Step key={label}>
                    <StepLabel>{label}</StepLabel>
                  </Step>
                ))}
              </Stepper>

              {activeStep === 0 && (
                <Box>
                  <TextField
                    fullWidth
                    margin="normal"
                    label="Subscription Name"
                    name="name"
                    value={formik.values.name}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    error={formik.touched.name && Boolean(formik.errors.name)}
                    helperText={formik.touched.name && formik.errors.name}
                  />

                  <TextField
                    fullWidth
                    margin="normal"
                    label="Description"
                    name="description"
                    value={formik.values.description}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    multiline
                    rows={3}
                  />

                  <TextField
                    fullWidth
                    margin="normal"
                    select
                    label="Category"
                    name="category"
                    value={formik.values.category}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    error={formik.touched.category && Boolean(formik.errors.category)}
                    helperText={formik.touched.category && formik.errors.category}
                  >
                    {SUBSCRIPTION_CATEGORIES.map((category) => (
                      <MenuItem key={category} value={category}>{category}</MenuItem>
                    ))}
                  </TextField>

                  <TextField
                    fullWidth
                    margin="normal"
                    select
                    label="Type"
                    name="type"
                    value={formik.values.type}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    error={formik.touched.type && Boolean(formik.errors.type)}
                    helperText={formik.touched.type && formik.errors.type}
                  >
                    <MenuItem value="recurring">Recurring</MenuItem>
                    <MenuItem value="one-time">One-time</MenuItem>
                  </TextField>

                  {formik.values.type === "recurring" && (
                    <Box sx={{ display: "flex", alignItems: "center", gap: 2, mt: 2, mb: 1 }}>
                      <Typography>Every</Typography>
                      <TextField
                        name="billingIntervalCount"
                        type="number"
                        value={formik.values.billingIntervalCount}
                        onChange={formik.handleChange}
                        onBlur={formik.handleBlur}
                        error={formik.touched.billingIntervalCount && Boolean(formik.errors.billingIntervalCount)}
                        helperText={formik.touched.billingIntervalCount && formik.errors.billingIntervalCount}
                        sx={{ width: 80 }}
                        InputProps={{ inputProps: { min: 1 } }}
                      />
                      <TextField
                        select
                        name="billingIntervalUnit"
                        value={formik.values.billingIntervalUnit}
                        onChange={formik.handleChange}
                        sx={{ minWidth: 120 }}
                      >
                        <MenuItem value="day">Day</MenuItem>
                        <MenuItem value="week">Week</MenuItem>
                        <MenuItem value="month">Month</MenuItem>
                        <MenuItem value="year">Year</MenuItem>
                      </TextField>
                    </Box>
                  )}
                </Box>
              )}

              {activeStep === 1 && (
                <Box>
                  <TextField
                    fullWidth
                    margin="normal"
                    label={`Amount (${formik.values.currency})`}
                    name="cost"
                    type="number"
                    value={formik.values.cost}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    error={formik.touched.cost && Boolean(formik.errors.cost)}
                    helperText={formik.touched.cost && formik.errors.cost}
                    sx={{
                      "& input[type=number]": {
                        MozAppearance: "textfield", // Firefox
                      },
                      "& input[type=number]::-webkit-outer-spin-button, & input[type=number]::-webkit-inner-spin-button":
                        {
                          WebkitAppearance: "none", // Chrome, Safari
                          margin: 0,
                        },
                    }}
                  />

                  <TextField
                    fullWidth
                    margin="normal"
                    select
                    label="Currency"
                    name="currency"
                    value={formik.values.currency}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    error={formik.touched.currency && Boolean(formik.errors.currency)}
                    helperText={formik.touched.currency && formik.errors.currency}
                  >
                    {SUPPORTED_CURRENCIES.map((currency) => (
                      <MenuItem key={currency} value={currency}>{currency}</MenuItem>
                    ))}
                  </TextField>

                  <TextField
                    fullWidth
                    margin="normal"
                    type="date"
                    label="Start Date"
                    name="startDate"
                    value={formik.values.startDate}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    error={formik.touched.startDate && Boolean(formik.errors.startDate)}
                    helperText={formik.touched.startDate && formik.errors.startDate}
                    InputLabelProps={{ shrink: true }}
                  />

                  <TextField
                    fullWidth
                    margin="normal"
                    label="Payment Method"
                    name="paymentMethod"
                    value={formik.values.paymentMethod}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                  />

                  {formik.values.type === "recurring" && (
                    <FormControlLabel
                      sx={{ mt: 1 }}
                      control={
                        <Switch
                          name="emailNotificationsEnabled"
                          checked={formik.values.emailNotificationsEnabled}
                          onChange={formik.handleChange}
                        />
                      }
                      label="Email me before this subscription renews"
                    />
                  )}

                  <TextField
                    fullWidth
                    margin="normal"
                    label="Website"
                    name="website"
                    value={formik.values.website}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                  />
                </Box>
              )}
            </>
          )}
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 2 }}>
          {activeStep === 0 ? (
            <>
              <Button onClick={handleDialogClose} disabled={loading} sx={{ color: theme.palette.text.primary }}>
                Cancel
              </Button>
              <Button variant="contained" onClick={handleNext} disabled={loading}>
                Next
              </Button>
            </>
          ) : (
            <>
              <Button onClick={handleBack} disabled={loading}>
                Back
              </Button>
              <Button variant="contained" onClick={() => formik.handleSubmit()} disabled={loading}>
                {editId ? "Save" : "Add"}
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>
    </>
  );
}
