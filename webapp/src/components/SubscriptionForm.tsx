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
} from "@mui/material";
import { useState } from "react";
import { createSubscription, getSubscriptionById } from "../api/subscription";
import * as Yup from "yup";
import { useFormik } from "formik";
import { useSnackbar } from "../utils/Snackbar";
import { useEffect } from "react";
import CircularProgress from "@mui/material/CircularProgress";
import { useTheme } from "@mui/material";
import { updateSubscriptionThunk, createSubscriptionThunk, fetchSubscriptionById } from "../app/subscriptionSlice";
import { useDispatch } from "react-redux";
import type { AppDispatch } from "../app/store";
import type { UpdateSubscriptionPayload, SubscriptionType, BillingUnit } from "../types/subscription";

type Props = {
  open: boolean;
  handleClose: () => void;
  onSuccess?: () => void;
  editId?: number | null;
};

type FormValues = {
  name: string;
  description: string;
  cost: number | "";
  type: SubscriptionType;
  category: string;
  startDate: string;
  paymentMethod: string;
  website: string;
  billingIntervalUnit: BillingUnit;
  billingIntervalCount: number;
};

export default function SubscriptionForm({ open, handleClose, onSuccess, editId }: Props) {
  const [loading, setLoading] = useState(false);
  const [activeStep, setActiveStep] = useState(0);
  const snackbar = useSnackbar();
  const theme = useTheme();
  const dispatch = useDispatch<AppDispatch>();

  const validationSchema = Yup.object({
    name: Yup.string().required("Name is required"),
    description: Yup.string(),
    cost: Yup.number().typeError("Amount must be a number").positive("Must be positive").required("Amount is required"),
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
      cost: 0,
      type: "recurring",
      category: "",
      startDate: "",
      paymentMethod: "",
      website: "",
      billingIntervalUnit: "month",
      billingIntervalCount: 1,
    },
    validationSchema,
    onSubmit: async (values) => {
      try {
        const payload = {
          id: editId,
          name: values.name,
          description: values.description,
          cost: Number(values.cost),
          type: values.type,
          category: values.category,
          startDate: values.startDate,
          paymentMethod: values.paymentMethod,
          website: values.website,
          billingIntervalUnit: values.billingIntervalUnit,
          billingIntervalCount: Number(values.billingIntervalCount),
        };

        if (editId) {
          const updatePayload: UpdateSubscriptionPayload = {
            id: editId,
            name: values.name,
            description: values.description,
            cost: Number(values.cost),
            type: values.type,
            category: values.category,
            startDate: values.startDate,
            paymentMethod: values.paymentMethod,
            website: values.website,
            billingIntervalUnit: values.billingIntervalUnit,
            billingIntervalCount: Number(values.billingIntervalCount),
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
            type: data.type || "recurring",
            category: data.category || "",
            startDate: data.startDate || "",
            paymentMethod: data.paymentMethod || "",
            website: data.website || "",
            billingIntervalUnit: data.billingIntervalUnit || "month",
            billingIntervalCount: data.billingIntervalCount || 1,
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
      formik.resetForm();
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
                    label="Category"
                    name="category"
                    value={formik.values.category}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    error={formik.touched.category && Boolean(formik.errors.category)}
                    helperText={formik.touched.category && formik.errors.category}
                  />

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
                    label="Amount ($)"
                    name="amount"
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
