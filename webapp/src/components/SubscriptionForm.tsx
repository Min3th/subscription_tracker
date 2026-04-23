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
} from "@mui/material";
import { useState } from "react";
import { createSubscription, updateSubscriptions, getSubscriptionById } from "../api/subscription";
import * as Yup from "yup";
import { useFormik } from "formik";
import { useSnackbar } from "../utils/Snackbar";
import { useEffect } from "react";
import CircularProgress from "@mui/material/CircularProgress";

type Props = {
  open: boolean;
  handleClose: () => void;
  onSuccess?: () => void;
  editId?: string | null;
};

export default function SubscriptionForm({ open, handleClose, onSuccess, editId }: Props) {
  const [loading, setLoading] = useState(false);
  const snackbar = useSnackbar();

  const validationSchema = Yup.object({
    name: Yup.string().required("Name is required"),
    description: Yup.string(),
    amount: Yup.number()
      .typeError("Amount must be a number")
      .positive("Must be positive")
      .required("Amount is required"),
    type: Yup.string().required("Type is required"),
    category: Yup.string().required("Category is required"),
    startDate: Yup.date().required("Date is required"),
    billingIntervalUnit: Yup.string().required("Required"),
    billingIntervalCount: Yup.number().positive("Must be positive").required("Required"),
  });

  const formik = useFormik({
    initialValues: {
      name: "",
      description: "",
      amount: "",
      type: "",
      category: "",
      startDate: "",
      paymentMethod: "",
      website: "",
      billingIntervalUnit: "month",
      billingIntervalCount: 1,
    },
    validationSchema,
    onSubmit: async (values) => {
      console.log("FORM SUBMITTED", values);
      try {
        const payload = {
          name: values.name,
          description: values.description,
          cost: Number(values.amount),
          type: values.type,
          category: values.category,
          startDate: values.startDate,
          paymentMethod: values.paymentMethod,
          website: values.website,
          billingIntervalUnit: values.billingIntervalUnit,
          billingIntervalCount: Number(values.billingIntervalCount),
        };

        if (editId) {
          await updateSubscriptions(editId, payload);
          snackbar.success("Subscription updated successfully!");
        } else {
          await createSubscription(payload);
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
    if (open && editId) {
      setLoading(true);
      getSubscriptionById(editId)
        .then((res) => {
          const data = res.data;
          formik.setValues({
            name: data.name || "",
            description: data.description || "",
            amount: data.cost || "",
            type: data.type || "recurring",
            category: data.category || "",
            startDate: data.startDate || "",
            paymentMethod: data.paymentMethod || "",
            website: data.website || "",
            billingIntervalUnit: data.billingIntervalUnit || "month",
            billingIntervalCount: data.billingIntervalCount || 1,
          });
        })
        .catch((err) => {
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

  const handleDialogClose = () => {
    formik.resetForm();
    handleClose();
  };
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
                label="Amount ($)"
                name="amount"
                type="number"
                value={formik.values.amount}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                error={formik.touched.amount && Boolean(formik.errors.amount)}
                helperText={formik.touched.amount && formik.errors.amount}
                sx={{
                  "& input[type=number]": {
                    MozAppearance: "textfield", // Firefox
                  },
                  "& input[type=number]::-webkit-outer-spin-button, & input[type=number]::-webkit-inner-spin-button": {
                    WebkitAppearance: "none", // Chrome, Safari
                    margin: 0,
                  },
                }}
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
            </>
          )}
        </DialogContent>

        <DialogActions>
          <Button onClick={handleDialogClose} disabled={loading}>
            Cancel
          </Button>
          <Button variant="contained" onClick={() => formik.handleSubmit()} disabled={loading}>
            {editId ? "Save" : "Add"}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
