import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import client, { API_BASE_URL } from "../api/client";
import AuthShell from "../components/AuthShell";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [error, setError] = useState("");
  const location = useLocation();
  const navigate = useNavigate();

  const socialProviders = [
    {
      key: "google",
      label: "Continue with Google",
      url: import.meta.env.VITE_GOOGLE_AUTH_URL || `${API_BASE_URL}/oauth2/authorization/google`
    }
  ];

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.get("oauthError")) {
      setError("OAuth login failed. Check provider credentials and callback settings.");
    }
    if (location.state?.successMessage) {
      setSuccessMessage(location.state.successMessage);
    }
  }, [location.search]);

  const clearMessages = () => {
    setError("");
    setSuccessMessage("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!username.trim() || !password.trim()) {
      setError("Username and password are required.");
      return;
    }

    try {
      setIsLoading(true);
      const response = await client.post("/auth/login/password", {
        username: username.trim(),
        password: password.trim()
      });
      localStorage.setItem("authToken", response.data.token);
      navigate("/upload");
    } catch (err) {
      setError(err.response?.data?.error || "Login failed. Check your credentials and try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSocialLogin = (providerName, authUrl) => {
    clearMessages();
    if (!authUrl) {
      setError(`${providerName} login is not configured yet. Add its URL in frontend/.env.`);
      return;
    }
    window.location.href = authUrl;
  };

  return (
    <AuthShell
      title="Sign In"
      subtitle="Use password login for your local account, or continue with a social provider."
    >
      <form onSubmit={handleSubmit} className="form">
        {error && <p className="error form-message">{error}</p>}

        <label htmlFor="username">Username</label>
        <input
          id="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Enter username"
          autoComplete="username"
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Enter password"
          autoComplete="current-password"
        />

        <button type="submit" disabled={isLoading}>
          {isLoading ? "Signing in..." : "Login"}
        </button>
      </form>

      <div className="auth-links auth-links-under-button">
        <Link to="/register">Register</Link>
        <Link to="/reset-password">Change password</Link>
      </div>

      <div className="divider" aria-hidden="true">
        <span>or</span>
      </div>

      <div className="social-grid">
        {socialProviders.map((provider) => (
          <button
            key={provider.key}
            type="button"
            className={`social-btn social-${provider.key}`}
            onClick={() => handleSocialLogin(provider.label, provider.url)}
          >
            {provider.label}
          </button>
        ))}
      </div>

      {successMessage && <p className="success">{successMessage}</p>}
    </AuthShell>
  );
}
