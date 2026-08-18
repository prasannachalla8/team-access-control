import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { login } from "../../api/authApi";
import { useAuthStore } from "../../store/authStore";
import { useOrgStore } from "../../store/orgStore";
import Button from "../../components/Button";
import "./auth.css";

export default function LoginPage() {
  const [email, setEmail] = useState("owner@test.com");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const setTokens = useAuthStore((s) => s.setTokens);
  const resetOrgs = useOrgStore((s) => s.reset);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const next = searchParams.get("next");

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await login({ email, password });
      setTokens(res.data);
      resetOrgs();
      navigate(next || "/dashboard");
    } catch (err) {
      setError(err.response?.data?.message || "Login failed. Check your credentials.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1 className="auth-title">Sign in</h1>
        <p className="auth-subtitle">Access your team's control panel</p>
        <label className="auth-label">EMAIL</label>
        <input className="auth-input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label className="auth-label">PASSWORD</label>
        <input className="auth-input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        {error && <div className="auth-error">{error}</div>}
        <Button type="submit" disabled={loading}>{loading ? "Signing in..." : "Sign in"}</Button>
        <p className="auth-footer">
          Don't have an account? <a href={`/signup${next ? `?next=${encodeURIComponent(next)}` : ""}`}>Create one</a>
        </p>
      </form>
    </div>
  );
}