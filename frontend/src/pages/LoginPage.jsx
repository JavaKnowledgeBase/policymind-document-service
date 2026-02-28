import { useState } from "react";
import { useNavigate } from "react-router-dom";
import client from "../api/client";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const socialProviders = [
    { key: "google", label: "Continue with Google", url: import.meta.env.VITE_GOOGLE_AUTH_URL },
    {
      key: "microsoft",
      label: "Continue with Microsoft",
      url: import.meta.env.VITE_MICROSOFT_AUTH_URL
    },
    { key: "facebook", label: "Continue with Facebook", url: import.meta.env.VITE_FACEBOOK_AUTH_URL },
    { key: "linkedin", label: "Continue with LinkedIn", url: import.meta.env.VITE_LINKEDIN_AUTH_URL },
    { key: "twitter", label: "Continue with X (Twitter)", url: import.meta.env.VITE_TWITTER_AUTH_URL }
  ];

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    if (!username.trim()) {
      setError("Username is required.");
      return;
    }

    try {
      setIsLoading(true);
      const response = await client.post("/auth/login", null, {
        params: { username: username.trim() }
      });
      localStorage.setItem("authToken", response.data);
      navigate("/upload");
    } catch (err) {
      setError("Login failed. Check backend and try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSocialLogin = (providerName, authUrl) => {
    setError("");
    if (!authUrl) {
      setError(`${providerName} login is not configured yet. Add its URL in frontend/.env.`);
      return;
    }
    window.location.href = authUrl;
  };

  return (
    <main className="page">
      <section className="card">
        <h1>PolicyMind Login</h1>
        <p>Sign in to upload policy documents.</p>

        <form onSubmit={handleSubmit} className="form">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Enter username"
            autoComplete="username"
          />

          <button type="submit" disabled={isLoading}>
            {isLoading ? "Signing in..." : "Login"}
          </button>
        </form>

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

        {error && <p className="error">{error}</p>}
      </section>
    </main>
  );
}
