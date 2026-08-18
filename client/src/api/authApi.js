import apiClient from "./client";

// POST /api/v1/auth/signup
// body: { email, password }
export function signup({ email, password }) {
  return apiClient.post("/api/v1/auth/signup", { email, password });
}

// POST /api/v1/auth/login
// body: { email, password }
// returns: { accessToken, refreshToken, tokenType }
export function login({ email, password }) {
  return apiClient.post("/api/v1/auth/login", { email, password });
}