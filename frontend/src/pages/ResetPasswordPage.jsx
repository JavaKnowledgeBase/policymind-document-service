import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import client from "../api/client";
import AuthShell from "../components/AuthShell";

const EMPTY_RESET_FORM = {
  username: "",
  securityAnswer: "",
  newPassword: ""
};

export default function ResetPasswordPage() {
  const [form, setForm] = useState(EMPTY_RESET_FORM);
  const [securityQuestion, setSecurityQuestion] = useState("");
  const [isFetchingQuestion, setIsFetchingQuestion] = useState(false);
  const [isResetting, setIsResetting] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const navigate = useNavigate();

  const clearMessages = () => {
    setError("");
    setSuccessMessage("");
  };

  const handleFetchQuestion = async () => {
    clearMessages();

    if (!form.username.trim()) {
      setError("Enter your username to fetch the security question.");
      return;
    }

    try {
      setIsFetchingQuestion(true);
      const response = await client.get("/auth/forgot-password/question", {
        params: { username: form.username.trim() }
      });
      setSecurityQuestion(response.data.securityQuestion || "");
      setSuccessMessage("Security question loaded. Answer it to choose a new password.");
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

    if (!form.username.trim() || !form.securityAnswer.trim() || !form.newPassword.trim()) {
      setError("Username, security answer, and new password are required.");
      return;
    }

    try {
      setIsResetting(true);
      await client.post("/auth/forgot-password/reset", {
        username: form.username.trim(),
        securityAnswer: form.securityAnswer.trim(),
        newPassword: form.newPassword.trim()
      });
      navigate("/", {
        replace: true,
        state: {
          successMessage: "Password reset successful. Sign in with your new password."
        }
      });
    } catch (err) {
      setError(err.response?.data?.error || "Password reset failed.");
    } finally {
      setIsResetting(false);
    }
  };

  return (
    <AuthShell
      title="Reset Password"
      subtitle="Load your saved security question, answer it, and set a new password."
    >
      <form onSubmit={handleResetPassword} className="form">
        <label htmlFor="reset-username">Username</label>
        <input
          id="reset-username"
          value={form.username}
          onChange={(e) => setForm((current) => ({ ...current, username: e.target.value }))}
          placeholder="Enter username"
          autoComplete="username"
        />

        <button type="button" className="secondary" onClick={handleFetchQuestion} disabled={isFetchingQuestion}>
          {isFetchingQuestion ? "Loading question..." : "Load Security Question"}
        </button>

        {securityQuestion && (
          <p className="security-question"><strong>Security question:</strong> {securityQuestion}</p>
        )}

        <label htmlFor="reset-answer">Security answer</label>
        <input
          id="reset-answer"
          type="password"
          value={form.securityAnswer}
          onChange={(e) => setForm((current) => ({ ...current, securityAnswer: e.target.value }))}
          placeholder="Enter answer"
          autoComplete="off"
        />

        <label htmlFor="reset-password">New password</label>
        <input
          id="reset-password"
          type="password"
          value={form.newPassword}
          onChange={(e) => setForm((current) => ({ ...current, newPassword: e.target.value }))}
          placeholder="Enter new password"
          autoComplete="new-password"
        />

        <button type="submit" disabled={isResetting}>
          {isResetting ? "Resetting..." : "Reset Password"}
        </button>
      </form>

      {successMessage && <p className="success">{successMessage}</p>}
      {error && <p className="error">{error}</p>}

      <div className="auth-links">
        <Link to="/">Back to login</Link>
        <Link to="/register">Need an account?</Link>
      </div>
    </AuthShell>
  );
}
