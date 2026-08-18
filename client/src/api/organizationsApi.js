import apiClient from "./client";

// POST /api/v1/organizations
// body: { name, slug }
export function createOrganization({ name, slug }) {
  return apiClient.post("/api/v1/organizations", { name, slug });
}

// GET /api/v1/organizations
// returns: [{ id, name, slug, roleName, createdAt }]
export function listMyOrganizations() {
  return apiClient.get("/api/v1/organizations");
}

// POST /api/v1/organizations/{orgId}/invite
// body: { email, roleName }
export function inviteMember(orgId, { email, roleName }) {
  return apiClient.post(`/api/v1/organizations/${orgId}/invite`, {
    email,
    roleName,
  });
}

// GET /api/v1/organizations/{orgId}/members?page=0&size=10
// returns: { content, page, size, totalElements, totalPages }
export function getMembers(orgId, page = 0, size = 10) {
  return apiClient.get(
    `/api/v1/organizations/${orgId}/members?page=${page}&size=${size}`
  );
}

// PUT /api/v1/organizations/{orgId}/members/{userId}/role?roleName=...
export function changeMemberRole(orgId, userId, roleName) {
  return apiClient.put(
    `/api/v1/organizations/${orgId}/members/${userId}/role?roleName=${encodeURIComponent(roleName)}`
  );
}

// DELETE /api/v1/organizations/{orgId}/members/{userId}
export function removeMember(orgId, userId) {
  return apiClient.delete(`/api/v1/organizations/${orgId}/members/${userId}`);
}

// POST /api/v1/organizations/accept-invite?token=...
// backend takes token as a @RequestParam, not a JSON body
export function acceptInvite(token) {
  return apiClient.post(
    `/api/v1/organizations/accept-invite?token=${encodeURIComponent(token)}`
  );
}

// GET /api/v1/organizations/{orgId}/audit-logs?page=0&size=10
// returns: { content, page, size, totalElements, totalPages }
export function getAuditLogs(orgId, page = 0, size = 10) {
  return apiClient.get(
    `/api/v1/organizations/${orgId}/audit-logs?page=${page}&size=${size}`
  );
}