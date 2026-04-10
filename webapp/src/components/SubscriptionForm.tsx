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
            value={form.name}
            onChange={handleChange}
          />

          <TextField
            fullWidth
            margin="normal"
            label="Amount ($)"
            name="amount"
            type="number"
            value={form.amount}
            onChange={handleChange}
          />

          <TextField
            select
            fullWidth
            margin="normal"
            label="Billing Cycle"
            name="billingCycle"
            value={form.billingCycle}
            onChange={handleChange}
          >
            <MenuItem value="monthly">Monthly</MenuItem>
            <MenuItem value="yearly">Yearly</MenuItem>
          </TextField>

          <TextField
            fullWidth
            margin="normal"
            label="Category"
            name="category"
            value={form.category}
            onChange={handleChange}
          />

          <TextField
            fullWidth
            margin="normal"
            type="date"
            label="Next Billing Date"
            name="nextBillingDate"
            value={form.nextBillingDate}
            onChange={handleChange}
            InputLabelProps={{ shrink: true }}
          />
        </DialogContent>

        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit}>
            Add
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
