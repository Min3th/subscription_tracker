import axios from "axios";
import { tokenStore } from "./tokenStore";

let requestCount = 0;
let hideLoaderTimer: ReturnType<typeof setTimeout> | null = null;

const showLoader = () => {
  if (hideLoaderTimer) {
    clearTimeout(hideLoaderTimer);
    hideLoaderTimer = null;
  }
  requestCount++;
  if (requestCount === 1) {
    window.dispatchEvent(new CustomEvent("show-loader"));
  }
};

const hideLoader = () => {
  requestCount = Math.max(0, requestCount - 1);
  if (requestCount === 0) {
    hideLoaderTimer = setTimeout(() => {
      window.dispatchEvent(new CustomEvent("hide-loader"));
    }, 100);
  }
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  withCredentials: true,
});

let refreshPromise: Promise<string> | null = null;

api.interceptors.request.use((config) => {
  showLoader();
  const token = tokenStore.get();
  if (token && !config.url?.includes("/auth/google") && !config.url?.includes("/auth/refresh")) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => {
    hideLoader();
    return res;
  },
  async (err) => {
    hideLoader();
    const originalRequest = err.config as typeof err.config & { _retry?: boolean };

    if (
      (err.response?.status === 401 || err.response?.status === 403) &&
      !originalRequest._retry &&
      !originalRequest.url?.includes("/auth/refresh") &&
      !originalRequest.url?.includes("/auth/google")
    ) {
      originalRequest._retry = true;
      try {
        if (!refreshPromise) {
          refreshPromise = api
            .post("/auth/refresh")
            .then((res) => {
              const newToken = res.data.accessToken as string;
              tokenStore.set(newToken);
              return newToken;
            })
            .finally(() => {
              refreshPromise = null;
            });
        }
        const newToken = await refreshPromise;
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        tokenStore.clear();
        window.location.href = "/";
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(err);
  },
);

export default api;
