import { useEffect, useState } from "react";
import { listRoles } from "../../api/rolesApi";

const roleColors = {
  owner: "#D6A34D",
  admin: "#5B8DEF",
  member: "#4CAF7D",
  viewer: "#8A93A3",
};

export default function RolesPage() {
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    listRoles()
      .then((res) => setRoles(res.data))
      .catch((err) =>
        setError(err.response?.data?.message || "Could not load roles.")
      )
      .finally(() => setLoading(false));
  }, []);

  // Union of every permission across all roles, so every card lists the
  // same rows in the same order — makes the grid actually comparable.
  const allPermissions = Array.from(
    new Set(roles.flatMap((r) => r.permissions))
  ).sort();

  return (
    <div>
      <h1 className="page-title">Roles & Permissions</h1>
      <p className="page-subtitle">Deny-by-default. Each role's clearance is explicit.</p>

      {error && <div className="auth-error">{error}</div>}
      {loading && <p style={{ color: "var(--text-muted)" }}>Loading...</p>}

      {!loading && !error && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16 }}>
          {roles.map((role) => {
            const color = roleColors[role.name] || "#8A93A3";
            return (
              <div key={role.id} className="card" style={{ overflow: "hidden" }}>
                <div style={{ height: 6, background: color }} />
                <div style={{ padding: 16 }}>
                  <div style={{ fontFamily: "var(--font-display)", fontWeight: 600, marginBottom: 2, textTransform: "capitalize" }}>
                    {role.name}
                  </div>
                  {role.description && (
                    <div style={{ fontSize: 11, color: "var(--text-muted)", marginBottom: 12 }}>
                      {role.description}
                    </div>
                  )}
                  {allPermissions.map((p) => {
                    const granted = role.permissions.includes(p);
                    return (
                      <div key={p} style={{ display: "flex", gap: 8, fontSize: 12, marginBottom: 6, alignItems: "center" }}>
                        <span
                          style={{
                            width: 10,
                            height: 10,
                            borderRadius: "50%",
                            flexShrink: 0,
                            background: granted ? color : "transparent",
                            border: `1px solid ${granted ? color : "var(--border)"}`,
                          }}
                        />
                        <span style={{ fontFamily: "var(--font-mono)", color: "var(--text-muted)" }}>{p}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}