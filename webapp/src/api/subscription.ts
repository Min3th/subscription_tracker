import api from "./client";

export const createSubscription = async (data: any) => {
  const response = await api.post("/subscriptions", data);
  return response;
};

export const getSubscriptions = async () => {
  return await api.get("/subscriptions");
};

export const getSubscriptionById = async (id: string) => {
  return await api.get(`/subscriptions/${id}`);
};

export const updateSubscriptions = async (id: string, data: any) => {
  const response = await api.put(`/subscriptions/${id}`, data);
  return response;
};

export const deleteSubscription = async (id: string) => {
  const response = await api.delete(`/subscriptions/${id}`);
  return response;
};
