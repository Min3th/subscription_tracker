import api from "./client";
import type {
  ConfirmSuggestionRequest,
  SubscriptionSuggestion,
} from "../types/suggestion";
import type { DetailedSubscription } from "../types/subscription";

export interface InboundEmailAddress {
  active: boolean;
  address: string | null;
  createdAt: string | null;
}

export const getInboundEmailAddress = async (): Promise<InboundEmailAddress> => {
  const response = await api.get<InboundEmailAddress>("/inbound-email/address");
  return response.data;
};

export const createInboundEmailAddress = async (): Promise<InboundEmailAddress> => {
  const response = await api.post<InboundEmailAddress>("/inbound-email/address");
  return response.data;
};

export const rotateInboundEmailAddress = async (): Promise<InboundEmailAddress> => {
  const response = await api.post<InboundEmailAddress>("/inbound-email/address/rotate");
  return response.data;
};

export const revokeInboundEmailAddress = async (): Promise<void> => {
  await api.delete("/inbound-email/address");
};

export const getPendingSuggestions = async (): Promise<SubscriptionSuggestion[]> => {
  const response = await api.get<SubscriptionSuggestion[]>("/inbound-email/suggestions");
  return response.data;
};

export const confirmSuggestion = async (
  id: string,
  request: ConfirmSuggestionRequest,
): Promise<DetailedSubscription> => {
  const response = await api.post<DetailedSubscription>(
    `/inbound-email/suggestions/${id}/confirm`,
    request,
  );
  return response.data;
};

export const ignoreSuggestion = async (id: string): Promise<void> => {
  await api.post(`/inbound-email/suggestions/${id}/ignore`);
};
