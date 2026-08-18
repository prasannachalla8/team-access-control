import { useEffect, useState } from "react";
import { listSessions, revokeSession } from "../../api/sessionsApi";
import Pagination from "../../components/Pagination";

export default function SessionsPage() {
  const [sessions, setSessions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  function load() {
    setLoading(true);
    setError(null);
    listSessions(page, 10)
      .then((res) => {
        setSessions(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message || "Could not load sessions."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  async function handleRevoke(id) {
    try {
      await revokeSession(id);
      load(); // refetch current page — a revoked row leaving mid-page is fine, refetch keeps counts accurate
    } catch (err) {
      setError(err.response?.data?.message || "Could not revoke session.");
    }
  }

  return (
    <div>
      <h1 className="page-title">Sessions</h1>
      <p className="page-subtitle">Active sessions tied to your account.</p>

      {error && <div className="auth-error">{error}</div>}
      {loading && <p style={{ color: "var(--text-muted)" }}>Loading...</p>}
      {!loading && sessions.length === 0 && <p style={{ color: "var(--text-muted)" }}>No active sessions.</p>}

      {!loading && sessions.length > 0 && (
        <div className="card">
          {sessions.map((s) => (
            <div
              key={s.id}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 16,
                padding: "14px 16px",
                borderBottom: "1px solid var(--border)",
                fontSize: 13,
              }}
            >
              <div style={{ flex: 1 }}>
                <div style={{ fontFamily: "var(--font-mono)", fontSize: 12 }}>
                  {s.userAgent || "unknown device"}
                </div>
                <div style={{ color: "var(--text-muted)", fontFamily: "var(--font-mono)", fontSize: 11, marginTop: 2 }}>
                  {s.ipAddress} · expires {s.expiresAt}
                </div>
              </div>
              <button
                onClick={() => handleRevoke(s.id)}
                style={{
                  background: "none",
                  border: "1px solid var(--border)",
                  color: "var(--danger)",
                  padding: "4px 10px",
                  borderRadius: 6,
                  fontSize: 12,
                  cursor: "pointer",
                }}
              >
                Revoke
              </button>
            </div>
          ))}
          <Pagination page={page} totalPages={totalPages} totalElements={totalElements} onPageChange={setPage} />
        </div>
      )}
    </div>
  );
}
