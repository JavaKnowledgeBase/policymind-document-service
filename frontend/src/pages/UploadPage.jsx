import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import client from "../api/client";

export default function UploadPage() {
  const [file, setFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("authToken");
    navigate("/");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!file) {
      setError("Please select a file.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
      setIsUploading(true);
      const response = await client.post("/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" }
      });
      setMessage(response.data || "File uploaded successfully.");
    } catch (err) {
      setError("Upload failed. Try again or check server logs.");
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <main className="page">
      <section className="card">
        <h1>Upload Document</h1>
        <p>Select a document and send it to the backend.</p>

        <form onSubmit={handleSubmit} className="form">
          <label htmlFor="file">Document file</label>
          <input
            id="file"
            type="file"
            accept=".pdf,.doc,.docx,.txt"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />

          <button type="submit" disabled={isUploading}>
            {isUploading ? "Uploading..." : "Upload"}
          </button>
        </form>

        {message && <p className="success">{message}</p>}
        {error && <p className="error">{error}</p>}

        <div className="actions">
          <button type="button" className="secondary" onClick={handleLogout}>
            Logout
          </button>
          <Link to="/error">Go to Error Page</Link>
        </div>
      </section>
    </main>
  );
}
