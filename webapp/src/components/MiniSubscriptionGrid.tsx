import { Grid } from "@mui/material";
import MiniSubscriptionCard from "./MiniSubscriptionCard";
import type { DetailedSubscription } from "../types/subscription";

interface SubscriptionGridProps {
  subscriptions: DetailedSubscription[];
  t: (key: string) => string;
}

export default function SubscriptionGrid({ subscriptions, t }: SubscriptionGridProps) {
  if (!subscriptions.length) return null;

  return (
    <Grid container spacing={3}>
      {subscriptions.map((sub) => (
        <Grid key={sub.id} size={{ xs: 12, md: 6, lg: 4 }} sx={{ minWidth: "300px" }}>
          <MiniSubscriptionCard sub={sub} t={t} />
        </Grid>
      ))}
    </Grid>
  );
}
