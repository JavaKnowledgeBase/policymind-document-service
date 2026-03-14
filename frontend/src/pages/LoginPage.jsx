import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import client, { API_BASE_URL } from "../api/client";
import BrandBar from "../components/BrandBar";
import DeveloperCredit from "../components/DeveloperCredit";

const EMPTY_REGISTER_FORM = {
  username: "",
  password: "",
  securityQuestion: "",
  securityAnswer: ""
};

const EMPTY_RESET_FORM = {
  username: "",
  securityAnswer: "",
  newPassword: ""
};

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [registerForm, setRegisterForm] = useState(EMPTY_REGISTER_FORM);
  const [resetForm, setResetForm] = useState(EMPTY_RESET_FORM);
  const [securityQuestion, setSecurityQuestion] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  const [isFetchingQuestion, setIsFetchingQuestion] = useState(false);
  const [isResetting, setIsResetting] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
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

  const handleRegister = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!registerForm.username.trim() || !registerForm.password.trim() || !registerForm.securityQuestion.trim() || !registerForm.securityAnswer.trim()) {
      setError("Registration requires username, password, security question, and security answer.");
      return;
    }

    try {
      setIsRegistering(true);
      const response = await client.post("/auth/register", {
        username: registerForm.username.trim(),
        password: registerForm.password.trim(),
        securityQuestion: registerForm.securityQuestion.trim(),
        securityAnswer: registerForm.securityAnswer.trim()
      });
      setSuccessMessage(response.data.message || "User registered successfully.");
      setRegisterForm(EMPTY_REGISTER_FORM);
    } catch (err) {
      setError(err.response?.data?.error || "Registration failed.");
    } finally {
      setIsRegistering(false);
    }
  };

  const handleFetchQuestion = async () => {
    clearMessages();

    if (!resetForm.username.trim()) {
      setError("Enter your username to fetch the security question.");
      return;
    }

    try {
      setIsFetchingQuestion(true);
      const response = await client.get("/auth/forgot-password/question", {
        params: { username: resetForm.username.trim() }
      });
      setSecurityQuestion(response.data.securityQuestion || "");
      setSuccessMessage("Security question loaded. Answer it to reset your password.");
    } catch (err) {
      setError(err.response?.data?.error || "Could not load security question.");
      setSecurityQuestion("");
    } finally {
      setIsFetchingQuestion(false);
    }
  };

  const handleResetPassword = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!resetForm.username.trim() || !resetForm.securityAnswer.trim() || !resetForm.newPassword.trim()) {
      setError("Username, security answer, and new password are required.");
      return;
    }

    try {
      setIsResetting(true);
      const response = await client.post("/auth/forgot-password/reset", {
        username: resetForm.username.trim(),
        securityAnswer: resetForm.securityAnswer.trim(),
        newPassword: resetForm.newPassword.trim()
      });
      setSuccessMessage(response.data.message || "Password reset successful.");
      setResetForm(EMPTY_RESET_FORM);
      setSecurityQuestion("");
    } catch (err) {
      setError(err.response?.data?.error || "Password reset failed.");
    } finally {
      setIsResetting(false);
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
            <p className="muted">Use password login, register a local account, or recover access with your security question.</p>

            <form onSubmit={handleSubmit} className="form">
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

            <div className="divider" aria-hidden="true">
              <span>register</span>
            </div>

            <form onSubmit={handleRegister} className="form">
              <label htmlFor="register-username">New username</label>
              <input
                id="register-username"
                value={registerForm.username}
                onChange={(e) => setRegisterForm((current) => ({ ...current, username: e.target.value }))}
                placeholder="Create username"
                autoComplete="username"
              />

              <label htmlFor="register-password">New password</label>
              <input
                id="register-password"
                type="password"
                value={registerForm.password}
                onChange={(e) => setRegisterForm((current) => ({ ...current, password: e.target.value }))}
                placeholder="Create password"
                autoComplete="new-password"
              />

              <label htmlFor="register-question">Security question</label>
              <input
                id="register-question"
                value={registerForm.securityQuestion}
                onChange={(e) => setRegisterForm((current) => ({ ...current, securityQuestion: e.target.value }))}
                placeholder="Example: First pet's name?"
              />

              <label htmlFor="register-answer">Security answer</label>
              <input
                id="register-answer"
                type="password"
                value={registerForm.securityAnswer}
                onChange={(e) => setRegisterForm((current) => ({ ...current, securityAnswer: e.target.value }))}
                placeholder="Enter answer"
                autoComplete="off"
              />

              <button type="submit" disabled={isRegistering}>
                {isRegistering ? "Registering..." : "Register"}
              </button>
            </form>

            <div className="divider" aria-hidden="true">
              <span>reset password</span>
            </div>

            <form onSubmit={handleResetPassword} className="form">
              <label htmlFor="reset-username">Username</label>
              <input
                id="reset-username"
                value={resetForm.username}
                onChange={(e) => setResetForm((current) => ({ ...current, username: e.target.value }))}
                placeholder="Enter username"
                autoComplete="username"
              />

              <button type="button" className="secondary" onClick={handleFetchQuestion} disabled={isFetchingQuestion}>
                {isFetchingQuestion ? "Loading question..." : "Load Security Question"}
              </button>

              {securityQuestion && (
                <p><strong>Security question:</strong> {securityQuestion}</p>
              )}

              <label htmlFor="reset-answer">Security answer</label>
              <input
                id="reset-answer"
                type="password"
                value={resetForm.securityAnswer}
                onChange={(e) => setResetForm((current) => ({ ...current, securityAnswer: e.target.value }))}
                placeholder="Enter answer"
                autoComplete="off"
              />

              <label htmlFor="reset-password">New password</label>
              <input
                id="reset-password"
                type="password"
                value={resetForm.newPassword}
                onChange={(e) => setResetForm((current) => ({ ...current, newPassword: e.target.value }))}
                placeholder="Enter new password"
                autoComplete="new-password"
              />

              <button type="submit" disabled={isResetting}>
                {isResetting ? "Resetting..." : "Reset Password"}
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

            {successMessage && <p className="success">{successMessage}</p>}
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
