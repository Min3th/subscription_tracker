import api from "./client";

export const createSubscription = async (data: any) => {
  const response = await api.post("/subscriptions", data);
  console.log("Response: ", response);
  return response;
};

export const getSubscriptions = async () => {
  return await api.get("/subscriptions");
};
