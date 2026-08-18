import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { acceptInvite } from "../../api/organizationsApi";
import { useAuthStore } from "../../store/authStore";
import Button from "../../components/Button";
import "./auth.css";

export default function AcceptInvitePage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const navigate = useNavigate();

  const [status, setStatus] = useState("idle");
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!token) {
      setStatus("error");
      setError("This invite link is missing its token.");
    }
  }, [token]);

  async function handleAccept() {
    setStatus("loading");
    setError(null);
    try {
      await acceptInvite(token);
      setStatus("success");
      setTimeout(() => navigate("/dashboard"), 1200);
    } catch (err) {
      setStatus("error");
      setError(err.response?.data?.message || "Could not accept this invitation.");
    }
  }

  if (!isAuthenticated) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <h1 className="auth-title">Almost there</h1>
          <p className="auth-subtitle">Log in or create an account with the email this invite was sent to, then come back to this link to join.</p>
          <Button onClick={() => navigate(`/login?next=/accept-invite?token=${token}`)}>Go to login</Button>
          <p className="auth-footer">No account yet? <a href="/signup">Sign up</a></p>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <h1 className="auth-title">Join organization</h1>
        <p className="auth-subtitle">You've been invited to join a team. Accept below to get access.</p>
        {status === "error" && <div className="auth-error">{error}</div>}
        {status === "success" && <div style={{ background: "rgba(76,175,125,0.1)", border: "1px solid rgba(76,175,125,0.3)", color: "var(--success)", fontSize: 13, padding: "8px 10px", borderRadius: 8, marginBottom: 16 }}>Joined successfully — redirecting...</div>}
        {status !== "success" && (
          <Button onClick={handleAccept} disabled={status === "loading" || !token}>
            {status === "loading" ? "Joining..." : "Accept invitation"}
          </Button>
        )}
      </div>
    </div>
  );
}