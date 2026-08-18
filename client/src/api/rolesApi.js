import apiClient from "./client";

// GET /api/v1/roles
// returns: [{ id, name, description, permissions: string[] }]
export function listRoles() {
  return apiClient.get("/api/v1/roles");
}