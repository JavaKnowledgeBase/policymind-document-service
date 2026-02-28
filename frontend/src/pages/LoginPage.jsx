import { useState } from "react";
import { useNavigate } from "react-router-dom";
import client from "../api/client";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

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

        {error && <p className="error">{error}</p>}
      </section>
    </main>
  );
}
