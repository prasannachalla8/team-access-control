import { useEffect, useState } from "react";
import { inviteMember, getMembers, changeMemberRole, removeMember } from "../../api/organizationsApi";
import { useAuthStore } from "../../store/authStore";
import { useCurrentOrg } from "../../store/orgStore";
import Button from "../../components/Button";
import Pagination from "../../components/Pagination";

export default function MembersPage() {
  const currentOrg = useCurrentOrg();
  const currentUser = useAuthStore((s) => s.user);

  const [email, setEmail] = useState("");
  const [roleName, setRoleName] = useState("member");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [loading, setLoading] = useState(false);

  const [members, setMembers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [membersLoading, setMembersLoading] = useState(true);
  const [membersError, setMembersError] = useState(null);
  const [rowActionError, setRowActionError] = useState(null);
  const [busyUserId, setBusyUserId] = useState(null);

  function loadMembers() {
    if (!currentOrg) return;
    setMembersLoading(true);
    setMembersError(null);
    getMembers(currentOrg.id, page, 10)
      .then((res) => {
        setMembers(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      })
      .catch((err) => setMembersError(err.response?.data?.message || "Could not load members."))
      .finally(() => setMembersLoading(false));
  }

  useEffect(() => {
    loadMembers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentOrg, page]);

  async function handleInvite(e) {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setLoading(true);
    try {
      await inviteMember(currentOrg.id, { email, roleName });
      setSuccess(`Invite sent to ${email}.`);
      setEmail("");
    } catch (err) {
      setError(err.response?.data?.message || "Could not send invite.");
    } finally {
      setLoading(false);
    }
  }

  async function handleRoleChange(userId, newRole) {
    setRowActionError(null);
    setBusyUserId(userId);
    try {
      await changeMemberRole(currentOrg.id, userId, newRole);
      loadMembers();
    } catch (err) {
      setRowActionError(err.response?.data?.message || "Could not change role.");
    } finally {
      setBusyUserId(null);
    }
  }

  async function handleRemove(userId, memberEmail) {
    if (!window.confirm(`Remove ${memberEmail} from ${currentOrg.name}?`)) return;
    setRowActionError(null);
    setBusyUserId(userId);
    try {
      await removeMember(currentOrg.id, userId);
      // If this was the last row on the current page (and not page 0),
      // step back a page so we don't land on an empty page.
      if (members.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        loadMembers();
      }
    } catch (err) {
      setRowActionError(err.response?.data?.message || "Could not remove member.");
    } finally {
      setBusyUserId(null);
    }
  }

  if (!currentOrg) {
    return (
      <div>
        <h1 className="page-title">Team</h1>
        <p className="page-subtitle">Create an organization first — go to Overview.</p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="page-title">Team</h1>
      <p className="page-subtitle">{currentOrg.name}</p>

      <form
        className="card"
        style={{ padding: 16, marginBottom: 16, display: "flex", gap: 8, alignItems: "flex-end" }}
        onSubmit={handleInvite}
      >
        <div style={{ flex: 1 }}>
          <label className="auth-label">EMAIL</label>
          <input
            className="auth-input"
            style={{ marginBottom: 0 }}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div>
          <label className="auth-label">ROLE</label>
          <select
            className="auth-input"
            style={{ marginBottom: 0 }}
            value={roleName}
            onChange={(e) => setRoleName(e.target.value)}
          >
            <option value="admin">Admin</option>
            <option value="member">Member</option>
            <option value="viewer">Viewer</option>
          </select>
        </div>
        <Button type="submit" disabled={loading} style={{ width: "auto", padding: "10px 20px" }}>
          {loading ? "Sending..." : "Invite"}
        </Button>
      </form>

      {error && <div className="auth-error">{error}</div>}
      {success && (
        <div
          style={{
            background: "rgba(76,175,125,0.1)",
            border: "1px solid rgba(76,175,125,0.3)",
            color: "var(--success)",
            fontSize: 13,
            padding: "8px 10px",
            borderRadius: 8,
            marginBottom: 16,
          }}
        >
          {success}
        </div>
      )}

      {membersError && <div className="auth-error">{membersError}</div>}
      {rowActionError && <div className="auth-error">{rowActionError}</div>}
      {membersLoading && <p style={{ color: "var(--text-muted)" }}>Loading members...</p>}

      {!membersLoading && members.length > 0 && (
        <div className="card">
          {members.map((m) => {
            const isSelf = m.userId === currentUser?.id;
            const isBusy = busyUserId === m.userId;
            return (
              <div
                key={m.userId}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "12px 16px",
                  borderBottom: "1px solid var(--border)",
                  fontSize: 13,
                }}
              >
                <span style={{ flex: 1 }}>
                  {m.email}
                  {isSelf && <span style={{ color: "var(--text-muted)", marginLeft: 6 }}>(you)</span>}
                </span>

                <select
                  className="auth-input"
                  style={{ marginBottom: 0, width: 110, padding: "6px 8px", fontSize: 12 }}
                  value={m.roleName}
                  disabled={isSelf || isBusy}
                  onChange={(e) => handleRoleChange(m.userId, e.target.value)}
                >
                  <option value="owner">Owner</option>
                  <option value="admin">Admin</option>
                  <option value="member">Member</option>
                  <option value="viewer">Viewer</option>
                </select>

                <button
                  onClick={() => handleRemove(m.userId, m.email)}
                  disabled={isSelf || isBusy}
                  style={{
                    background: "none",
                    border: "1px solid var(--border)",
                    color: isSelf ? "var(--text-muted)" : "var(--danger)",
                    padding: "5px 10px",
                    borderRadius: 6,
                    fontSize: 12,
                    cursor: isSelf ? "not-allowed" : "pointer",
                    opacity: isBusy ? 0.5 : 1,
                  }}
                >
                  Remove
                </button>
              </div>
            );
          })}
          <Pagination page={page} totalPages={totalPages} totalElements={totalElements} onPageChange={setPage} />
        </div>
      )}
    </div>
  );
}
