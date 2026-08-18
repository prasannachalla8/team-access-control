export default function Pagination({ page, totalPages, totalElements, onPageChange }) {
  if (totalPages <= 1) return null;
  const canPrev = page > 0;
  const canNext = page < totalPages - 1;
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "10px 16px", borderTop: "1px solid var(--border)", fontSize: 12, fontFamily: "var(--font-mono)", color: "var(--text-muted)" }}>
      <span>Page {page + 1} of {totalPages} · {totalElements} total</span>
      <div style={{ display: "flex", gap: 8 }}>
        <button onClick={() => onPageChange(page - 1)} disabled={!canPrev} style={pageBtnStyle(canPrev)}>Prev</button>
        <button onClick={() => onPageChange(page + 1)} disabled={!canNext} style={pageBtnStyle(canNext)}>Next</button>
      </div>
    </div>
  );
}

function pageBtnStyle(enabled) {
  return { background: "none", border: "1px solid var(--border)", color: enabled ? "var(--text)" : "var(--text-muted)", padding: "4px 10px", borderRadius: 6, fontSize: 12, cursor: enabled ? "pointer" : "not-allowed", opacity: enabled ? 1 : 0.5, fontFamily: "var(--font-mono)" };
}