import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import client from "../api/client";
import AuthShell from "../components/AuthShell";
import { executeRecaptcha, preloadRecaptcha } from "../lib/recaptcha";

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

  useEffect(() => {
    preloadRecaptcha();
  }, []);

  const handleRegister = async (event) => {
    event.preventDefault();
    setError("");

    if (!form.username.trim() || !form.password.trim() || !form.securityQuestion.trim() || !form.securityAnswer.trim()) {
      setError("Complete all fields to create your account.");
      return;
    }

    try {
      setIsRegistering(true);
      const recaptchaToken = await executeRecaptcha("register");
      await client.post("/auth/register", {
        username: form.username.trim(),
        password: form.password.trim(),
        securityQuestion: form.securityQuestion.trim(),
        securityAnswer: form.securityAnswer.trim(),
        recaptchaToken
      });
      navigate("/", {
        replace: true,
        state: {
          successMessage: "Your account is ready. You can sign in now."
        }
      });
    } catch (err) {
      const statusCode = err.response?.status;
      const apiError = err.response?.data?.error;
      const networkMessage = err.message;
      const fallbackMessage = "We could not finish creating your account.";
      const detail = apiError || networkMessage || fallbackMessage;

      setError(statusCode ? `We could not create your account (${statusCode}). ${detail}` : detail);
    } finally {
      setIsRegistering(false);
    }
  };

  return (
    <AuthShell
      title="Create Account"
      subtitle="Create an account so you can upload documents and review them securely."
    >
      <form onSubmit={handleRegister} className="form">
        <label htmlFor="register-username">Username</label>
        <input
          id="register-username"
          value={form.username}
          onChange={(e) => setForm((current) => ({ ...current, username: e.target.value }))}
          placeholder="Choose a username"
          autoComplete="username"
        />

        <label htmlFor="register-password">Password</label>
        <input
          id="register-password"
          type="password"
          value={form.password}
          onChange={(e) => setForm((current) => ({ ...current, password: e.target.value }))}
          placeholder="Create a password"
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
          placeholder="Enter your answer"
          autoComplete="off"
        />

        <button type="submit" disabled={isRegistering}>
          {isRegistering ? "Creating account..." : "Create Account"}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      <div className="auth-links">
        <Link to="/">Back to sign in</Link>
      </div>
    </AuthShell>
  );
}
