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
import { createSubscription } from "../api/subscription";
import * as Yup from "yup";
import { useFormik } from "formik";
import { useSnackbar } from "../utils/Snackbar";

type Props = {
  open: boolean;
  handleClose: () => void;
};

export default function SubscriptionForm({ open, handleClose }: Props) {
  const snackbar = useSnackbar();

  const validationSchema = Yup.object({
    name: Yup.string().required("Name is required"),
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
          cost: Number(values.amount),
          type: values.type,
          category: values.category,
          startDate: values.startDate,
          paymentMethod: values.paymentMethod,
          website: values.website,
          billingIntervalUnit: values.billingIntervalUnit,
          billingIntervalCount: Number(values.billingIntervalCount),
        };

        await createSubscription(payload);
        snackbar.success("Subscription created successfully!");
        handleClose();
      } catch (error) {
        console.error("Error creating subscription:", error);
        snackbar.error("Failed to create subscription. Please try again.");
      }
    },
  });

  const handleDialogClose = () => {
    formik.resetForm();
    handleClose();
  };
  return (
    <>
      <Dialog open={open} onClose={handleDialogClose} fullWidth maxWidth="sm">
        <DialogTitle>Add Subscription</DialogTitle>

        <DialogContent>
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
        </DialogContent>

        <DialogActions>
          <Button onClick={handleDialogClose}>Cancel</Button>
          <Button variant="contained" onClick={() => formik.handleSubmit()}>
            Add
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
