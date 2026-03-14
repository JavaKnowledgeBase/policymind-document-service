import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import client from "../api/client";
import AuthShell from "../components/AuthShell";

const EMPTY_REGISTER_FORM = {
  username: "",
  password: "",
  securityQuestion: "",
  securityAnswer: ""
};

export default function RegisterPage() {
  const [form, setForm] = useState(EMPTY_REGISTER_FORM);
  const [isRegistering, setIsRegistering] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleRegister = async (event) => {
    event.preventDefault();
    setError("");

    if (!form.username.trim() || !form.password.trim() || !form.securityQuestion.trim() || !form.securityAnswer.trim()) {
      setError("Registration requires username, password, security question, and security answer.");
      return;
    }

    try {
      setIsRegistering(true);
      await client.post("/auth/register", {
        username: form.username.trim(),
        password: form.password.trim(),
        securityQuestion: form.securityQuestion.trim(),
        securityAnswer: form.securityAnswer.trim()
      });
      navigate("/", {
        replace: true,
        state: {
          successMessage: "Account created. You can sign in now."
        }
      });
    } catch (err) {
      const statusCode = err.response?.status;
      const apiError = err.response?.data?.error;
      const networkMessage = err.message;
      const fallbackMessage = "Registration failed before the request completed.";
      const detail = apiError || networkMessage || fallbackMessage;

      setError(statusCode ? `Registration failed (${statusCode}): ${detail}` : detail);
    } finally {
      setIsRegistering(false);
    }
  };

  return (
    <AuthShell
      title="Create Account"
      subtitle="Set up a local PolicyMind account with a security question for password recovery."
    >
      <form onSubmit={handleRegister} className="form">
        <label htmlFor="register-username">New username</label>
        <input
          id="register-username"
          value={form.username}
          onChange={(e) => setForm((current) => ({ ...current, username: e.target.value }))}
          placeholder="Create username"
          autoComplete="username"
        />

        <label htmlFor="register-password">New password</label>
        <input
          id="register-password"
          type="password"
          value={form.password}
          onChange={(e) => setForm((current) => ({ ...current, password: e.target.value }))}
          placeholder="Create password"
          autoComplete="new-password"
        />

        <label htmlFor="register-question">Security question</label>
        <input
          id="register-question"
          value={form.securityQuestion}
          onChange={(e) => setForm((current) => ({ ...current, securityQuestion: e.target.value }))}
          placeholder="Example: First pet's name?"
        />

        <label htmlFor="register-answer">Security answer</label>
        <input
          id="register-answer"
          type="password"
          value={form.securityAnswer}
          onChange={(e) => setForm((current) => ({ ...current, securityAnswer: e.target.value }))}
          placeholder="Enter answer"
          autoComplete="off"
        />

        <button type="submit" disabled={isRegistering}>
          {isRegistering ? "Registering..." : "Register"}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      <div className="auth-links">
        <Link to="/">Back to login</Link>
      </div>
    </AuthShell>
  );
}
