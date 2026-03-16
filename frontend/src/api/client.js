import axios from "axios";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

function normalizeStoredToken(token) {
  if (!token) {
    return "";
  }

  return String(token)
    .trim()
    .replace(/^Bearer\s+/i, "")
    .trim();
}

const client = axios.create({
  baseURL: API_BASE_URL
});

client.interceptors.request.use((config) => {
  const requestUrl = String(config.url || "");
  const isAuthRequest = requestUrl.startsWith("/auth/");
  const method = String(config.method || "get").toUpperCase();
  config.headers = config.headers || {};

  if (isAuthRequest && config.headers?.Authorization) {
    delete config.headers.Authorization;
  }

  if (isAuthRequest) {
    return config;
  }

  const token = localStorage.getItem("authToken");
  if (token) {
    const normalizedToken = normalizeStoredToken(token);
    if (normalizedToken) {
      config.headers.Authorization = `Bearer ${normalizedToken}`;
    } else {
      localStorage.removeItem("authToken");
    }
  }

  config.headers["Cache-Control"] = "no-store, no-cache, max-age=0, must-revalidate";
  config.headers.Pragma = "no-cache";
  config.headers.Expires = "0";

  if (method === "GET") {
    config.params = {
      ...(config.params || {}),
      _ts: Date.now()
    };
  }

  return config;
});

export default client;
