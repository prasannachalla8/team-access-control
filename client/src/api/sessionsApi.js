import apiClient from "./client";

// GET /api/v1/sessions
// returns: { content, page, size, totalElements, totalPages }
export function listSessions(page = 0, size = 10) {
  return apiClient.get("/api/v1/sessions");
}

// DELETE /api/v1/sessions/{sessionId}
export function revokeSession(sessionId) {
  return apiClient.delete(`/api/v1/sessions/${sessionId}`);
}