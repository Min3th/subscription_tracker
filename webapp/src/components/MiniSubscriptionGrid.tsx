import { Grid } from "@mui/material";
import MiniSubscriptionCard from "./MiniSubscriptionCard";
import type { Subscription } from "../types/subscription";

interface SubscriptionGridProps {
  subscriptions: Subscription[];
  t: (key: string) => string;
  getNextBillingDate: (startDate: string, unit: string, count: number) => Date;
}

export default function SubscriptionGrid({ subscriptions, t, getNextBillingDate }: SubscriptionGridProps) {
  if (!subscriptions.length) return null;

  return (
    <Grid container spacing={3}>
      {subscriptions.map((sub) => (
        <Grid key={sub.id} size={{ xs: 12, md: 6, lg: 4 }}>
          <MiniSubscriptionCard sub={sub} t={t} getNextBillingDate={getNextBillingDate} />
        </Grid>
      ))}
    </Grid>
  );
}
