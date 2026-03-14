import axios from "axios";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

const client = axios.create({
  baseURL: API_BASE_URL
});

client.interceptors.request.use((config) => {
  const requestUrl = String(config.url || "");
  const isAuthRequest = requestUrl.startsWith("/auth/");

  if (isAuthRequest && config.headers?.Authorization) {
    delete config.headers.Authorization;
  }

  if (isAuthRequest) {
    return config;
  }

  const token = localStorage.getItem("authToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default client;
