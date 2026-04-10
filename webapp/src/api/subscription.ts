import api from "./client";

export const createSubscription = async (data: any) => {
  return await api.post("/subscriptions", data);
};

export const getSubscriptions = async () => {
  return await api.get("/subscriptions");
};
