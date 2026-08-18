import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import { useOrgStore, useCurrentOrg } from "../../store/orgStore";
import { listMyOrganizations } from "../../api/organizationsApi";
import "./dashboard.css";

const navItems = [
  { to: "/dashboard", label: "Overview", end: true },
  { to: "/dashboard/members", label: "Team" },
  { to: "/dashboard/roles", label: "Roles & Permissions" },
  { to: "/dashboard/sessions", label: "Sessions" },
  { to: "/dashboard/audit", label: "Audit Log" },
];

export default function DashboardLayout() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  const organizations = useOrgStore((s) => s.organizations);
  const setOrganizations = useOrgStore((s) => s.setOrganizations);
  const setCurrentOrgId = useOrgStore((s) => s.setCurrentOrgId);
  const resetOrgs = useOrgStore((s) => s.reset);
  const currentOrg = useCurrentOrg();

  const [orgSwitcherOpen, setOrgSwitcherOpen] = useState(false);
  const navigate = useNavigate();

 useEffect(() => {
  console.log("FETCHING ORGS...");
  listMyOrganizations()
    .then((res) => {
      console.log("ORGS RESPONSE", res.data);
      setOrganizations(res.data);
    })
    .catch((err) => {
      console.log("ORGS FETCH ERROR", err);
      setOrganizations([]);
    });
}, [setOrganizations]);
  function handleLogout() {
    logout();
    resetOrgs();
    navigate("/login");
  }

  const initials = user?.email ? user.email[0].toUpperCase() : "?";

  return (
    <div className="dash-shell">
      <aside className="dash-sidebar">
        <div className="dash-logo"><span className="dash-logo-mark">◆</span><span>Clearance</span></div>
        <nav className="dash-nav">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end}
              className={({ isActive }) => "dash-nav-item" + (isActive ? " active" : "")}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <button className="dash-logout" onClick={handleLogout}>Sign out</button>
      </aside>

      <div className="dash-main">
        <header className="dash-topbar">
          <div style={{ position: "relative" }}>
            <button className="dash-org-pill" style={{ cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}
              onClick={() => setOrgSwitcherOpen((v) => !v)}>
              {currentOrg?.name || "No organization yet"}
              {organizations.length > 1 && <span style={{ fontSize: 10 }}>▾</span>}
            </button>
            {orgSwitcherOpen && organizations.length > 0 && (
              <div style={{ position: "absolute", top: "calc(100% + 4px)", left: 0, background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 8, minWidth: 180, zIndex: 10 }}>
                {organizations.map((org) => (
                  <button key={org.id} onClick={() => { setCurrentOrgId(org.id); setOrgSwitcherOpen(false); }}
                    style={{ display: "block", width: "100%", textAlign: "left", padding: "8px 12px", background: "none", border: "none", color: "var(--text)", fontSize: 13, cursor: "pointer" }}>
                    {org.name} <span style={{ color: "var(--text-muted)", fontFamily: "var(--font-mono)", fontSize: 11 }}>({org.roleName})</span>
                  </button>
                ))}
              </div>
            )}
          </div>
          <div className="dash-avatar">{initials}</div>
        </header>
        <main className="dash-content"><Outlet /></main>
      </div>
    </div>
  );
}