import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import client from "../api/client";
import AuthShell from "../components/AuthShell";
import { executeRecaptcha, preloadRecaptcha } from "../lib/recaptcha";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [error, setError] = useState("");
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.get("oauthError")) {
      const provider = params.get("oauthProvider");
      const reason = params.get("oauthReason");
      const providerLabel = provider ? `${provider.charAt(0).toUpperCase()}${provider.slice(1)} ` : "";
      const reasonSuffix = reason ? ` Reason: ${reason}` : "";
      setError(`${providerLabel}sign-in did not complete. Please check the provider settings and try again.${reasonSuffix}`);
    }
    if (location.state?.successMessage) {
      setSuccessMessage(location.state.successMessage);
    }
  }, [location.search]);

  useEffect(() => {
    preloadRecaptcha();
  }, []);

  const clearMessages = () => {
    setError("");
    setSuccessMessage("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!username.trim() || !password.trim()) {
      setError("Enter your username and password to continue.");
      return;
    }

    try {
      setIsLoading(true);
      const recaptchaToken = await executeRecaptcha("login");
      const response = await client.post("/auth/login/password", {
        username: username.trim(),
        password: password.trim(),
        recaptchaToken
      });
      localStorage.setItem("authToken", response.data.token);
      navigate("/upload");
    } catch (err) {
      setError(err.response?.data?.error || "We could not sign you in. Please check your details and try again.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthShell
      title="Sign In"
      subtitle="Sign in to continue to your document review workspace."
    >
      <form onSubmit={handleSubmit} className="form">
        {error && <p className="error form-message">{error}</p>}

        <label htmlFor="username">Username</label>
        <input
          id="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Enter your username"
          autoComplete="username"
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Enter your password"
          autoComplete="current-password"
        />

        <button type="submit" disabled={isLoading}>
          {isLoading ? "Signing in..." : "Sign In"}
        </button>
      </form>

      <div className="auth-links auth-links-under-button">
        <Link to="/register">Create account</Link>
        <Link to="/reset-password">Reset password</Link>
      </div>
      {successMessage && <p className="success">{successMessage}</p>}
    </AuthShell>
  );
}
