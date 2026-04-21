import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/",
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      try {
        const res = await api.post("/auth/refresh");
        const newToken = res.data.accessToken;
        localStorage.setItem("token", newToken);
        err.config.headers["Authorization"] = `Bearer ${newToken}`;
        return api(err.config);
      } catch {
        localStorage.clear();
        window.location.href = "/";
      }
    }
    return Promise.reject(err);
  },
);

export default api;
