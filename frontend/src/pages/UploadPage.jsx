import { useEffect, useRef, useState } from "react";
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
  const summary = String(response?.summary || "").trim().toLowerCase();
  const hasStructuredContent =
    summary.length > 0 ||
    (response?.key_risks || []).length > 0 ||
    (response?.recommended_actions || []).length > 0;
  const invalidPhrases = [
    "no answer available",
    "answer not provided",
    "could not generate full",
    "service temporarily unavailable"
  ];

  if (!answer && !hasStructuredContent) {
    return false;
  }

  const invalidAnswer = answer && invalidPhrases.some((phrase) => answer.includes(phrase));
  const invalidSummary = summary && invalidPhrases.some((phrase) => summary.includes(phrase));

  return !(invalidAnswer && invalidSummary);
}

function toStatusTone(status) {
  if (status === "COMPLETED") {
    return "success";
  }
  if (status === "FAILED") {
    return "danger";
  }
  if (status === "PROCESSING" || status === "QUEUED") {
    return "warning";
  }
  return "neutral";
}

export default function UploadPage() {
  const [file, setFile] = useState(null);
  const [documentId, setDocumentId] = useState("");
  const [question, setQuestion] = useState("");
  const [answerData, setAnswerData] = useState(null);
  const [documentStatus, setDocumentStatus] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isAsking, setIsAsking] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const fileInputRef = useRef(null);
  const uploadRequestIdRef = useRef(0);
  const statusRequestIdRef = useRef(0);
  const askRequestIdRef = useRef(0);
  const navigate = useNavigate();
  const openAiResponse = answerData?.providers?.openai || answerData?.structuredOutput || null;
  const vertexResponse = answerData?.providers?.vertex || null;
  const statusLabel = documentStatus?.status || "Waiting for upload";
  const statusTone = toStatusTone(documentStatus?.status);
  const canAskQuestion = Boolean(documentId && documentStatus?.status === "COMPLETED");

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
  const leadAnalyst = rankedAnalysts[0] || null;

  const handleLogout = () => {
    localStorage.removeItem("authToken");
    navigate("/");
  };

  const loadDocumentStatus = async (id) => {
    const requestId = ++statusRequestIdRef.current;
    const response = await client.get(`/documents/${id}`);
    if (requestId !== statusRequestIdRef.current) {
      return;
    }
    setDocumentStatus(response.data);
  };

  useEffect(() => {
    if (!documentId || !documentStatus || !["QUEUED", "PROCESSING"].includes(documentStatus.status)) {
      return undefined;
    }

    const pollStatus = async () => {
      try {
        await loadDocumentStatus(documentId);
      } catch (err) {
        // Keep the last known status visible and let the user retry manually if needed.
      }
    };

    const intervalId = window.setInterval(pollStatus, 3000);
    return () => window.clearInterval(intervalId);
  }, [documentId, documentStatus]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");
    setAnswerData(null);
    setDocumentStatus(null);

    if (!file) {
      setError("Choose a file to get started.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    const requestId = ++uploadRequestIdRef.current;

    try {
      setIsUploading(true);
      const response = await client.post("/upload", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
          "X-Upload-Request-Id": `upload-${Date.now()}-${requestId}`
        }
      });
      if (requestId !== uploadRequestIdRef.current) {
        return;
      }
      const data = response.data;
      if (typeof data === "string") {
        setMessage(data);
      } else {
        const fileName = data?.fileName || "document";
        const documentId = data?.documentId ?? "N/A";
        if (data?.documentId) {
          setDocumentId(String(data.documentId));
        }
        setDocumentStatus({
          documentId: data?.documentId ?? null,
          fileName,
          status: data?.status || "QUEUED",
          chunksStored: data?.chunksStored ?? 0
        });
        setMessage(
          `${fileName} has been uploaded. Document ID: ${documentId}. We are processing it now in the background.`
        );
      }
    } catch (err) {
      if (requestId !== uploadRequestIdRef.current) {
        return;
      }
      const statusCode = err.response?.status;
      const apiError = err.response?.data?.error;
      const detail = apiError || err.message || "The upload did not finish successfully.";
      setError(statusCode ? `We could not upload that file (${statusCode}). ${detail}` : detail);
    } finally {
      if (requestId === uploadRequestIdRef.current) {
        setIsUploading(false);
      }
      setFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleAsk = async (event) => {
    event.preventDefault();
    setError("");
    setAnswerData(null);

    if (!documentId) {
      setError("Enter a document ID before asking a question.");
      return;
    }

    if (!question.trim()) {
      setError("Type a question about the document.");
      return;
    }

    if (documentStatus && documentStatus.status !== "COMPLETED") {
      setError("This document is still being prepared. Please wait until processing is complete.");
      return;
    }

    const requestId = ++askRequestIdRef.current;

    try {
      setIsAsking(true);
      const response = await client.post(`/${documentId}/ask`, {
        question,
        answerProvider: "both"
      }, {
        headers: {
          "X-Question-Request-Id": `ask-${Date.now()}-${requestId}`
        }
      });
      if (requestId !== askRequestIdRef.current) {
        return;
      }
      setAnswerData(response.data);
    } catch (err) {
      if (requestId !== askRequestIdRef.current) {
        return;
      }
      setError("We could not answer that question right now. Please try again in a moment.");
    } finally {
      if (requestId === askRequestIdRef.current) {
        setIsAsking(false);
      }
    }
  };

  return (
    <main className="page">
      <div className="page-orb page-orb-left" aria-hidden="true" />
      <div className="page-orb page-orb-right" aria-hidden="true" />
      <section className="card card-wide">
        <BrandBar />
        <div className="workspace-hero">
          <div>
            <p className="eyebrow">Review Workspace</p>
            <h1>Upload, track, and review with confidence</h1>
            <p className="muted workspace-lead">
              Bring in a document, follow its processing status, and ask focused questions once the review is ready.
            </p>
          </div>
          <div className="workspace-summary">
            <span className={`status-pill status-${statusTone}`}>{statusLabel}</span>
            <p>{canAskQuestion ? "Your document is ready for questions." : "Upload a file to begin the review flow."}</p>
          </div>
        </div>

        <div className="workspace-grid">
          <section className="workspace-main">
            <div className="workspace-panel">
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Step 1</p>
                  <h2>Upload a document</h2>
                </div>
                <span className="panel-note">PDF, Word, or text files</span>
              </div>
              <p className="muted">
                Choose a file and we will start processing it in the background so you can track progress right away.
              </p>

              <form onSubmit={handleSubmit} className="form">
                <label htmlFor="file">Document file</label>
                <input
                  ref={fileInputRef}
                  id="file"
                  type="file"
                  accept=".pdf,.doc,.docx,.txt"
                  onChange={(e) => setFile(e.target.files?.[0] || null)}
                />

                <button type="submit" disabled={isUploading}>
                  {isUploading ? "Uploading..." : "Upload Document"}
                </button>
              </form>
            </div>

            <div className="workspace-panel">
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Step 2</p>
                  <h2>Ask a question</h2>
                </div>
                <span className="panel-note">Ready after processing completes</span>
              </div>
              <p className="muted">
                Ask about risks, responsibilities, exclusions, or any other important part of the document.
              </p>

              <form onSubmit={handleAsk} className="form">
                <label htmlFor="documentId">Document ID</label>
                <input
                  id="documentId"
                  type="text"
                  value={documentId}
                  onChange={(e) => setDocumentId(e.target.value)}
                  placeholder="Document ID from upload response"
                />

                <label htmlFor="question">Your question</label>
                <textarea
                  id="question"
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  placeholder="Example: What are the key compliance risks in this policy?"
                  rows={4}
                />

                <button type="submit" disabled={isAsking}>
                  {isAsking ? "Reviewing..." : "Ask PolicyMind"}
                </button>
              </form>
            </div>
          </section>

          <aside className="workspace-sidebar">
            <div className="workspace-panel sidebar-panel">
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Live Status</p>
                  <h2>Document progress</h2>
                </div>
              </div>
              <div className="status-stack">
                <div className="status-row">
                  <span>Status</span>
                  <strong>{statusLabel}</strong>
                </div>
                <div className="status-row">
                  <span>Document ID</span>
                  <strong>{documentStatus?.documentId || documentId || "Not assigned yet"}</strong>
                </div>
                <div className="status-row">
                  <span>File name</span>
                  <strong>{documentStatus?.fileName || file?.name || "No file selected"}</strong>
                </div>
                <div className="status-row">
                  <span>Chunks</span>
                  <strong>{documentStatus?.chunksStored ?? 0}</strong>
                </div>
                <div className="status-row">
                  <span>Completed</span>
                  <strong>{documentStatus?.completedAt || "In progress"}</strong>
                </div>
              </div>
              <button type="button" className="secondary" onClick={() => loadDocumentStatus(documentId)} disabled={!documentId}>
                Refresh Status
              </button>
              {documentStatus?.errorMessage && (
                <p className="error"><strong>Processing Error:</strong> {documentStatus.errorMessage}</p>
              )}
            </div>

            <div className="workspace-panel sidebar-panel">
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Quick Tips</p>
                  <h2>Best results</h2>
                </div>
              </div>
              <ul className="feature-list compact-list">
                <li>Use a clean policy, contract, or guideline document.</li>
                <li>Wait for the status to show completed before asking questions.</li>
                <li>Ask direct questions like coverage limits, deadlines, or compliance risks.</li>
              </ul>
            </div>
          </aside>
        </div>

        {!!documentStatus && (
          <section className="rag-section">
            <div className="rag-head">
              <h2>Processing Status</h2>
              <div className="rag-badges">
                <span className="badge">Status: {documentStatus.status || "UNKNOWN"}</span>
                <span className="badge">Chunks: {documentStatus.chunksStored ?? 0}</span>
              </div>
            </div>
            <p><strong>Document ID:</strong> {documentStatus.documentId || documentId}</p>
            <p><strong>File:</strong> {documentStatus.fileName || "document"}</p>
            {documentStatus.completedAt && <p><strong>Completed:</strong> {documentStatus.completedAt}</p>}
            {documentStatus.errorMessage && <p className="error"><strong>Processing Error:</strong> {documentStatus.errorMessage}</p>}
            <button type="button" className="secondary" onClick={() => loadDocumentStatus(documentId)}>
              Refresh Status
            </button>
          </section>
        )}

        {message && <p className="success">{message}</p>}
        {error && <p className="error">{error}</p>}
        {(!!rankedAnalysts.length || !!answerData) && (
          <section className="rag-result">
            <div className="rag-head">
              <div>
                <p className="eyebrow">Review Results</p>
                <h2>PolicyMind Analysis</h2>
              </div>
              {leadAnalyst && (
                <div className="rag-badges">
                  <span className="badge">Top Risk Score: {leadAnalyst.response?.risk_score ?? "N/A"}</span>
                  <span className="badge">Confidence: {normalizeConfidence(leadAnalyst.response?.confidence)}</span>
                </div>
              )}
            </div>

            {!rankedAnalysts.length && (
              <div className="rag-section">
                <h3>No review available yet</h3>
                <p>We could not generate a clear answer yet. Try a more specific question or make sure the document has finished processing.</p>
              </div>
            )}

            {rankedAnalysts.map((analyst, idx) => (
              <div className="rag-section analyst-card" key={`analyst-${analyst.source}`}>
                <div className="rag-head">
                  <div>
                    <p className="eyebrow">Analyst {idx + 1}</p>
                    <h3>{analyst.source === "openai" ? "Primary Review" : "Secondary Review"}</h3>
                  </div>
                  <div className="rag-badges">
                    <span className="badge">Risk Score: {analyst.response?.risk_score ?? "N/A"}</span>
                    <span className="badge">
                      Confidence: {normalizeConfidence(analyst.response?.confidence)}
                    </span>
                  </div>
                </div>
                <div className="analysis-hero-card">
                  <div>
                    <p className="analysis-label">Quick take</p>
                    <p className="analysis-summary">{analyst.response?.summary || "No summary available."}</p>
                  </div>
                  <div>
                    <p className="analysis-label">Detailed answer</p>
                    <p className="analysis-answer">{analyst.response?.answer || "No answer available."}</p>
                  </div>
                </div>
                <div className="rag-grid">
                  <div className="rag-section insight-card">
                    <h3>Key Risks</h3>
                    {(analyst.response?.key_risks || []).length ? (
                      <ul>
                        {(analyst.response?.key_risks || []).map((risk, riskIdx) => (
                          <li key={`risk-${idx}-${riskIdx}`}>{risk}</li>
                        ))}
                      </ul>
                    ) : (
                      <p>No major risks stood out in this review.</p>
                    )}
                  </div>
                  <div className="rag-section insight-card">
                    <h3>Recommended Actions</h3>
                    {(analyst.response?.recommended_actions || []).length ? (
                      <ul>
                        {(analyst.response?.recommended_actions || []).map((action, actionIdx) => (
                          <li key={`action-${idx}-${actionIdx}`}>{action}</li>
                        ))}
                      </ul>
                    ) : (
                      <p>No immediate follow-up actions were suggested.</p>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {!!answerData?.retrievedChunkPreviews?.length && (
              <div className="rag-section evidence-panel">
                <div className="panel-head">
                  <div>
                    <p className="eyebrow">Supporting Evidence</p>
                    <h3>What the answer was based on</h3>
                  </div>
                </div>
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
            <span>Upload, process, search, and answer in one guided flow.</span>
          </div>
          <div className="metric-card">
            <strong>Architecture</strong>
            <span>Fast, reliable document review powered by secure services.</span>
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
