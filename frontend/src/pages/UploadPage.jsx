import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import client from "../api/client";
import BrandBar from "../components/BrandBar";
import DeveloperCredit from "../components/DeveloperCredit";

const CONFIDENCE_RANK = {
  high: 3,
  medium: 2,
  low: 1
};

function normalizeConfidence(confidence) {
  if (!confidence) {
    return "low";
  }
  const normalized = String(confidence).trim().toLowerCase();
  return CONFIDENCE_RANK[normalized] ? normalized : "low";
}

function toRiskScore(response) {
  const value = Number(response?.risk_score);
  return Number.isFinite(value) ? value : 0;
}

function toResponseLength(response) {
  const summaryLength = (response?.summary || "").length;
  const answerLength = (response?.answer || "").length;
  return summaryLength + answerLength;
}

function isUsableResponse(response) {
  const answer = String(response?.answer || "").trim().toLowerCase();
  const invalidPhrases = [
    "no answer available",
    "answer not provided",
    "could not generate full",
    "service temporarily unavailable"
  ];

  if (!answer) {
    return false;
  }

  return !invalidPhrases.some((phrase) => answer.includes(phrase));
}

export default function UploadPage() {
  const [file, setFile] = useState(null);
  const [documentId, setDocumentId] = useState("");
  const [question, setQuestion] = useState("");
  const [answerData, setAnswerData] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isAsking, setIsAsking] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const openAiResponse = answerData?.providers?.openai || answerData?.structuredOutput || null;
  const vertexResponse = answerData?.providers?.vertex || null;

  const rankedAnalysts = [
    openAiResponse && { source: "openai", response: openAiResponse },
    vertexResponse && { source: "vertex", response: vertexResponse }
  ]
    .filter((item) => item && isUsableResponse(item.response))
    .sort((a, b) => {
      const confidenceDiff =
        CONFIDENCE_RANK[normalizeConfidence(b.response?.confidence)] -
        CONFIDENCE_RANK[normalizeConfidence(a.response?.confidence)];
      if (confidenceDiff !== 0) {
        return confidenceDiff;
      }

      const riskDiff = toRiskScore(b.response) - toRiskScore(a.response);
      if (riskDiff !== 0) {
        return riskDiff;
      }

      return toResponseLength(b.response) - toResponseLength(a.response);
    });

  const handleLogout = () => {
    localStorage.removeItem("authToken");
    navigate("/");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");
    setAnswerData(null);

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
      const data = response.data;
      if (typeof data === "string") {
        setMessage(data);
      } else {
        const fileName = data?.fileName || "document";
        const documentId = data?.documentId ?? "N/A";
        const chunksStored = data?.chunksStored ?? 0;
        if (data?.documentId) {
          setDocumentId(String(data.documentId));
        }
        setMessage(
          `Uploaded ${fileName}. Document ID: ${documentId}. Chunks stored: ${chunksStored}.`
        );
      }
    } catch (err) {
      setError("Upload failed. Try again or check server logs.");
    } finally {
      setIsUploading(false);
    }
  };

  const handleAsk = async (event) => {
    event.preventDefault();
    setError("");
    setAnswerData(null);

    if (!documentId) {
      setError("Document ID is required.");
      return;
    }

    if (!question.trim()) {
      setError("Please enter a question.");
      return;
    }

    try {
      setIsAsking(true);
      const response = await client.post(`/${documentId}/ask`, {
        question,
        answerProvider: "both"
      });
      setAnswerData(response.data);
    } catch (err) {
      setError("Question failed. Check backend logs and AI provider settings.");
    } finally {
      setIsAsking(false);
    }
  };

  return (
    <main className="page">
      <section className="card card-wide">
        <BrandBar />
        <h1>Document Intelligence Workspace</h1>
        <p className="muted">
          Upload policy or contract files to trigger the retrieval pipeline: chunking, embedding, indexing,
          and explainable AI analysis.
        </p>

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

        <form onSubmit={handleAsk} className="form">
          <label htmlFor="documentId">Document ID</label>
          <input
            id="documentId"
            type="text"
            value={documentId}
            onChange={(e) => setDocumentId(e.target.value)}
            placeholder="Document ID from upload response"
          />

          <label htmlFor="question">Ask a question</label>
          <textarea
            id="question"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Example: What are the key compliance risks in this policy?"
            rows={4}
          />

          <button type="submit" disabled={isAsking}>
            {isAsking ? "Asking..." : "Ask PolicyMind"}
          </button>
        </form>

        {message && <p className="success">{message}</p>}
        {error && <p className="error">{error}</p>}
        {(!!rankedAnalysts.length || !!answerData) && (
          <section className="rag-result">
            <div className="rag-head">
              <h2>PolicyMind Analysis</h2>
            </div>

            {!rankedAnalysts.length && (
              <div className="rag-section">
                <h3>Analyst Status</h3>
                <p>Our analysts are not able to answer this question.</p>
              </div>
            )}

            {rankedAnalysts.map((analyst, idx) => (
              <div className="rag-section" key={`analyst-${analyst.source}`}>
                <div className="rag-head">
                  <h3>Analyst {idx + 1}</h3>
                  <div className="rag-badges">
                    <span className="badge">Risk Score: {analyst.response?.risk_score ?? "N/A"}</span>
                    <span className="badge">
                      Confidence: {normalizeConfidence(analyst.response?.confidence)}
                    </span>
                  </div>
                </div>
                <div className="rag-grid">
                  <div className="rag-section">
                    <h3>Summary and Answer</h3>
                    <p><strong>Summary:</strong> {analyst.response?.summary || "No summary available."}</p>
                    <p><strong>Answer:</strong> {analyst.response?.answer || "No answer available."}</p>
                  </div>
                  <div className="rag-section">
                    <h3>Key Risks</h3>
                    <ul>
                      {(analyst.response?.key_risks || []).map((risk, riskIdx) => (
                        <li key={`risk-${idx}-${riskIdx}`}>{risk}</li>
                      ))}
                    </ul>
                  </div>
                  <div className="rag-section">
                    <h3>Recommended Actions</h3>
                    <ul>
                      {(analyst.response?.recommended_actions || []).map((action, actionIdx) => (
                        <li key={`action-${idx}-${actionIdx}`}>{action}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>
            ))}

            {!!answerData?.retrievedChunkPreviews?.length && (
              <div className="rag-section">
                <h3>Retrieved Evidence</h3>
                {answerData.retrievedChunkPreviews.map((preview, idx) => (
                  <article className="evidence-card" key={`chunk-${idx}`}>
                    <strong>{answerData.retrievedLineRanges?.[idx] || `Line reference ${idx + 1}`}</strong>
                    <p>{preview}</p>
                  </article>
                ))}
              </div>
            )}
          </section>
        )}

        <div className="metrics-row upload-bottom-metrics">
          <div className="metric-card">
            <strong>Pipeline</strong>
            <span>Upload to Chunk to Embed to Retrieve to Explain</span>
          </div>
          <div className="metric-card">
            <strong>Architecture</strong>
            <span>Microservices + Redis cache + vector search</span>
          </div>
          <div className="metric-card">
            <strong>Audience</strong>
            <span>Insurance agencies, legal teams, and contract reviewers</span>
          </div>
        </div>

        <div className="actions upload-actions">
          <Link to="/about">Architecture Overview</Link>
          <button type="button" className="secondary" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </section>
      <DeveloperCredit />
    </main>
  );
}
