import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import client, { API_BASE_URL } from "../api/client";
import BrandBar from "../components/BrandBar";
import DeveloperCredit from "../components/DeveloperCredit";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const location = useLocation();
  const navigate = useNavigate();

  const socialProviders = [
    {
      key: "google",
      label: "Continue with Google",
      url: import.meta.env.VITE_GOOGLE_AUTH_URL || `${API_BASE_URL}/oauth2/authorization/google`
    },
    {
      key: "microsoft",
      label: "Continue with Microsoft",
      url: import.meta.env.VITE_MICROSOFT_AUTH_URL || `${API_BASE_URL}/oauth2/authorization/microsoft`
    },
    {
      key: "facebook",
      label: "Continue with Facebook",
      url: import.meta.env.VITE_FACEBOOK_AUTH_URL || `${API_BASE_URL}/oauth2/authorization/facebook`
    },
    {
      key: "linkedin",
      label: "Continue with LinkedIn",
      url: import.meta.env.VITE_LINKEDIN_AUTH_URL || `${API_BASE_URL}/oauth2/authorization/linkedin`
    },
    {
      key: "twitter",
      label: "Continue with X (Twitter)",
      url: import.meta.env.VITE_TWITTER_AUTH_URL || `${API_BASE_URL}/oauth2/authorization/twitter`
    }
  ];

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.get("oauthError")) {
      setError("OAuth login failed. Check provider credentials and callback settings.");
    }
  }, [location.search]);

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
      <section className="card card-wide">
        <BrandBar />
        <div className="vision-shell">
          <article className="vision-panel">
            <p className="eyebrow">PolicyMind AI</p>
            <h1>Understand Complex Policies and Contracts in Minutes</h1>
            <p>
              AI-powered policy intelligence platform built on microservices, RAG retrieval, vector search,
              and secure cloud-ready architecture.
            </p>
            <ul className="feature-list">
              <li>Simplifies legal and insurance language for faster decisions.</li>
              <li>Highlights risk areas and clause-level insights for each document.</li>
              <li>Supports growth from freemium users to enterprise agency workflows.</li>
            </ul>
          </article>

          <article className="login-panel">
            <h2>Sign In</h2>
            <p className="muted">Access upload, analysis, and risk insight workflows.</p>

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

          </article>
        </div>
      </section>
      <div className="login-bottom-left">
        <p>RAG + Explainability</p>
        <p>JWT + Secure APIs</p>
        <p>Redis + Vector Search</p>
      </div>
      <DeveloperCredit />
    </main>
  );
}
