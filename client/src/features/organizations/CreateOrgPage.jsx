import { useState } from "react";
import { createOrganization } from "../../api/organizationsApi";
import { useOrgStore } from "../../store/orgStore";
import Button from "../../components/Button";

function slugify(name) {
  return name
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");
}

export default function CreateOrgPage() {
  const [name, setName] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const setCurrentOrg = useOrgStore((s) => s.setCurrentOrg);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await createOrganization({ name, slug: slugify(name) });
      // res.data = { id, name, slug, createdAt }
      setCurrentOrg(res.data);
    } catch (err) {
      setError(err.response?.data?.message || "Could not create organization.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h1 className="page-title">Create your organization</h1>
      <p className="page-subtitle">
        You'll be the Owner. You can invite teammates right after.
      </p>
      <form
        onSubmit={handleSubmit}
        className="card"
        style={{ padding: 20, maxWidth: 400 }}
      >
        <label className="auth-label">ORGANIZATION NAME</label>
        <input
          className="auth-input"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Acme Robotics"
          required
        />
        {name && (
          <p style={{ fontSize: 12, color: "var(--text-muted)", marginTop: -10, marginBottom: 16, fontFamily: "var(--font-mono)" }}>
            slug: {slugify(name)}
          </p>
        )}
        {error && <div className="auth-error">{error}</div>}
        <Button type="submit" disabled={loading}>
          {loading ? "Creating..." : "Create organization"}
        </Button>
      </form>
    </div>
  );
}