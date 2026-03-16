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
      setError("Enter your username to load your security question.");
      return;
    }

    try {
      setIsFetchingQuestion(true);
      const response = await client.get("/auth/forgot-password/question", {
        params: { username: form.username.trim() }
      });
      setSecurityQuestion(response.data.securityQuestion || "");
      setSuccessMessage("Your security question is ready. Answer it to set a new password.");
    } catch (err) {
      setError(err.response?.data?.error || "We could not load your security question.");
      setSecurityQuestion("");
    } finally {
      setIsFetchingQuestion(false);
    }
  };

  const handleResetPassword = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!form.username.trim() || !form.securityAnswer.trim() || !form.newPassword.trim()) {
      setError("Complete all fields before resetting your password.");
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
          successMessage: "Your password has been updated. Sign in with your new password."
        }
      });
    } catch (err) {
      setError(err.response?.data?.error || "We could not reset your password. Please try again.");
    } finally {
      setIsResetting(false);
    }
  };

  return (
    <AuthShell
      title="Reset Password"
      subtitle="Answer your security question and choose a new password."
    >
      <form onSubmit={handleResetPassword} className="form">
        <label htmlFor="reset-username">Username</label>
        <input
          id="reset-username"
          value={form.username}
          onChange={(e) => setForm((current) => ({ ...current, username: e.target.value }))}
          placeholder="Enter your username"
          autoComplete="username"
        />

        <button type="button" className="secondary" onClick={handleFetchQuestion} disabled={isFetchingQuestion}>
          {isFetchingQuestion ? "Loading question..." : "Load Question"}
        </button>

        {securityQuestion && (
          <p className="security-question"><strong>Your security question:</strong> {securityQuestion}</p>
        )}

        <label htmlFor="reset-answer">Security answer</label>
        <input
          id="reset-answer"
          type="password"
          value={form.securityAnswer}
          onChange={(e) => setForm((current) => ({ ...current, securityAnswer: e.target.value }))}
          placeholder="Enter your answer"
          autoComplete="off"
        />

        <label htmlFor="reset-password">New password</label>
        <input
          id="reset-password"
          type="password"
          value={form.newPassword}
          onChange={(e) => setForm((current) => ({ ...current, newPassword: e.target.value }))}
          placeholder="Create a new password"
          autoComplete="new-password"
        />

        <button type="submit" disabled={isResetting}>
          {isResetting ? "Updating password..." : "Update Password"}
        </button>
      </form>

      {successMessage && <p className="success">{successMessage}</p>}
      {error && <p className="error">{error}</p>}

      <div className="auth-links">
        <Link to="/">Back to sign in</Link>
        <Link to="/register">Need an account?</Link>
      </div>
    </AuthShell>
  );
}
