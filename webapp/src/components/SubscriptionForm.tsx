import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  MenuItem,
  duration,
} from "@mui/material";
import { useState } from "react";
import { createSubscription } from "../api/subscription";
import * as Yup from "yup";
import { useFormik } from "formik";

type Props = {
  open: boolean;
  handleClose: () => void;
};

export default function SubscriptionForm({ open, handleClose }: Props) {
  const [form, setForm] = useState({
    name: "",
    amount: "",
    billingCycle: "monthly",
    category: "",
    nextBillingDate: "",
  });

  const validationSchema = Yup.object({
    name: Yup.string().required("Name is required"),
    amount: Yup.number()
      .typeError("Amount must be a number")
      .positive("Must be positive")
      .required("Amount is required"),
    billingCycle: Yup.string().required("Billing cycle required"),
    category: Yup.string().required("Category is required"),
    nextBillingDate: Yup.date().required("Date is required"),
  });

  const formik = useFormik({
    initialValues: {
      name: "",
      amount: "",
      billingCycle: "monthly",
      category: "",
      nextBillingDate: "",
    },
    validationSchema,
    onSubmit: async (values) => {
      try {
        const payload = {
          name: values.name,
          cost: Number(values.amount),
          duration: values.billingCycle,
          type: values.category,
        };

        await createSubscription(payload);
        handleClose();
      } catch (error) {
        console.error("Error creating subscription:", error);
      }
    },
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = () => {
    console.log("Form Data:", form);
    try {
      const payload = {
        name: form.name,
        cost: Number(form.amount),
        duration: form.billingCycle,
        type: form.category,
      };
      createSubscription(payload);
    } catch (error) {
      console.error("Error creating subscription:", error);
    }
    handleClose();
  };
  return (
    <>
      <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
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
          />

          <TextField
            select
            fullWidth
            margin="normal"
            label="Billing Cycle"
            name="billingCycle"
            value={formik.values.billingCycle}
            onChange={formik.handleChange}
          >
            <MenuItem value="monthly">Monthly</MenuItem>
            <MenuItem value="yearly">Yearly</MenuItem>
          </TextField>

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
            type="date"
            label="Next Billing Date"
            name="nextBillingDate"
            value={formik.values.nextBillingDate}
            onChange={formik.handleChange}
            onBlur={formik.handleBlur}
            error={formik.touched.nextBillingDate && Boolean(formik.errors.nextBillingDate)}
            helperText={formik.touched.nextBillingDate && formik.errors.nextBillingDate}
            InputLabelProps={{ shrink: true }}
          />
        </DialogContent>

        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button variant="contained" onClick={() => formik.handleSubmit()}>
            Add
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
