import axios from "axios";

const api = axios.create({
  baseURL: "/api", // for proxy
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token && !config.url?.includes("/auth/google") && !config.url?.includes("/auth/refresh")) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const originalRequest = err.config;

    // if (originalRequest._retry || originalRequest.url?.includes("/auth/refresh")) {
    //   return Promise.reject(err);
    // }
    if (err.response?.status === 401 || err.response?.status === 403) {
      originalRequest._retry = true;
      try {
        const res = await api.post("/auth/refresh");
        const newToken = res.data.accessToken;
        localStorage.setItem("token", newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(err.config);
      } catch (refreshError) {
        localStorage.clear();
        window.location.href = "/";
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(err);
  },
);

export default api;
