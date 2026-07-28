import api from "./client";

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
