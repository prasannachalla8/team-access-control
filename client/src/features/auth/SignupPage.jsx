import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { signup } from "../../api/authApi";
import Button from "../../components/Button";
import "./auth.css";

export default function SignupPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const next = searchParams.get("next");

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);

    if (password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }

    setLoading(true);
    try {
      await signup({ email, password });
      // signup doesn't log you in automatically (no tokens returned) —
      // send them to login (carrying the invite token forward, if any)
      // to get a fresh accessToken/refreshToken.
      navigate(`/login${next ? `?next=${encodeURIComponent(next)}` : ""}`);
    } catch (err) {
      setError(err.response?.data?.message || "Signup failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1 className="auth-title">Create your account</h1>
        <p className="auth-subtitle">
          This creates your login. You'll set up an organization next.
        </p>

        <label className="auth-label">EMAIL</label>
        <input
          className="auth-input"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <label className="auth-label">PASSWORD</label>
        <input
          className="auth-input"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
        />

        {error && <div className="auth-error">{error}</div>}

        <Button type="submit" disabled={loading}>
          {loading ? "Creating account..." : "Create account"}
        </Button>

        <p className="auth-footer">
          Already have an account? <a href="/login">Sign in</a>
        </p>
      </form>
    </div>
  );
}