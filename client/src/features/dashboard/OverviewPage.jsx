import { useState } from "react";
import { createOrganization } from "../../api/organizationsApi";
import { useOrgStore, useCurrentOrg } from "../../store/orgStore";
import Button from "../../components/Button";

export default function OverviewPage() {
  const currentOrg = useCurrentOrg();
  const loaded = useOrgStore((s) => s.loaded);
  const addOrganization = useOrgStore((s) => s.addOrganization);
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleCreateOrg(e) {
    e.preventDefault();
    setError(null); setLoading(true);
    try {
      const res = await createOrganization({ name, slug });
      addOrganization({ ...res.data, roleName: "owner" });
    } catch (err) {
      setError(err.response?.data?.message || "Could not create organization.");
    } finally { setLoading(false); }
  }

  if (!loaded) {
    return <p style={{ color: "var(--text-muted)" }}>Loading your organizations...</p>;
  }

  if (!currentOrg) {
    return (
      <div>
        <h1 className="page-title">Create your organization</h1>
        <p className="page-subtitle">You don't belong to any organization yet.</p>
        <form className="card" style={{ maxWidth: 360, padding: 20 }} onSubmit={handleCreateOrg}>
          <label className="auth-label">NAME</label>
          <input className="auth-input" value={name} onChange={(e) => setName(e.target.value)} required />
          <label className="auth-label">SLUG</label>
          <input className="auth-input" value={slug} onChange={(e) => setSlug(e.target.value)} placeholder="acme-robotics" required />
          {error && <div className="auth-error">{error}</div>}
          <Button type="submit" disabled={loading}>{loading ? "Creating..." : "Create organization"}</Button>
        </form>
      </div>
    );
  }

  return (
    <div>
      <h1 className="page-title">Overview</h1>
      <p className="page-subtitle">{currentOrg.name} · your role: {currentOrg.roleName}</p>
      <div className="card" style={{ padding: 16 }}>
        <div style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--text-muted)" }}>ORG ID: {currentOrg.id}</div>
        <div style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--text-muted)", marginTop: 4 }}>CREATED: {currentOrg.createdAt}</div>
      </div>
    </div>
  );
}