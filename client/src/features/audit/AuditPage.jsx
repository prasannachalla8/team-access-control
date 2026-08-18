import { useEffect, useState } from "react";
import { getAuditLogs } from "../../api/organizationsApi";
import { useCurrentOrg } from "../../store/orgStore";
import Pagination from "../../components/Pagination";

export default function AuditPage() {
  const currentOrg = useCurrentOrg();
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!currentOrg) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getAuditLogs(currentOrg.id, page, 10)
      .then((res) => {
        setLogs(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      })
      .catch((err) => setError(err.response?.data?.message || "Could not load audit logs."))
      .finally(() => setLoading(false));
  }, [currentOrg, page]);

  if (!currentOrg) {
    return (
      <div>
        <h1 className="page-title">Audit Log</h1>
        <p className="page-subtitle">Create an organization first — go to Overview.</p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="page-title">Audit Log</h1>
      <p className="page-subtitle">Every access-relevant event for {currentOrg.name}.</p>

      {error && <div className="auth-error">{error}</div>}
      {loading && <p style={{ color: "var(--text-muted)" }}>Loading...</p>}
      {!loading && logs.length === 0 && <p style={{ color: "var(--text-muted)" }}>No audit events yet.</p>}

      {!loading && logs.length > 0 && (
        <div className="card">
          <div style={{ padding: 16, fontFamily: "var(--font-mono)", fontSize: 12 }}>
            {logs.map((log) => (
              <div
                key={log.id}
                style={{ display: "flex", gap: 12, padding: "8px 0", borderBottom: "1px solid var(--border)" }}
              >
                <span style={{ color: "var(--text-muted)", width: 160, flexShrink: 0 }}>{log.createdAt}</span>
                <span style={{ width: 140, flexShrink: 0 }}>{log.action}</span>
                <span style={{ color: "var(--text-muted)", width: 160, flexShrink: 0 }}>{log.userEmail}</span>
                <span>{log.details}</span>
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={totalPages} totalElements={totalElements} onPageChange={setPage} />
        </div>
      )}
    </div>
  );
}
