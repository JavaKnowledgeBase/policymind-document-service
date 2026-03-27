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
const MAX_UPLOAD_SIZE_MB = 50;
const MAX_UPLOAD_SIZE_BYTES = MAX_UPLOAD_SIZE_MB * 1024 * 1024;

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

function normalizeProcessingErrorMessage(message) {
  const text = String(message || "").trim();
  if (!text) {
    return "";
  }

  if (text === "No readable text extracted from the uploaded file.") {
    return "We could not find readable text in that PDF. It may be a scanned or image-only file, so please try a searchable PDF or run OCR first.";
  }

  return text;
}

function normalizeUploadErrorMessage(statusCode, message, fileName) {
  if (statusCode === 413) {
    return `“${fileName || "This file"}” is larger than the current upload limit. Please choose a file under ${MAX_UPLOAD_SIZE_MB} MB.`;
  }

  if (String(message || "").includes("Only PDF files are supported right now.")) {
    return "Only PDF files are supported right now. Please choose a .pdf file.";
  }

  return message || "The upload did not finish successfully.";
}

export default function UploadPage() {
  const supportedFileMessage = "Only PDF files are supported right now.";
  const [activeStep, setActiveStep] = useState(1);
  const [file, setFile] = useState(null);
  const [documentId, setDocumentId] = useState("");
  const [question, setQuestion] = useState("");
  const [answerData, setAnswerData] = useState(null);
  const [documentStatus, setDocumentStatus] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isAsking, setIsAsking] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const workspaceTopRef = useRef(null);
  const stepTwoRef = useRef(null);
  const questionInputRef = useRef(null);
  const fileInputRef = useRef(null);
  const uploadRequestIdRef = useRef(0);
  const statusRequestIdRef = useRef(0);
  const askRequestIdRef = useRef(0);
  const shouldScrollToStepTwoRef = useRef(false);
  const navigate = useNavigate();
  const openAiResponse = answerData?.providers?.openai || answerData?.structuredOutput || null;
  const vertexResponse = answerData?.providers?.vertex || null;
  const statusLabel = documentStatus?.status || "Waiting for upload";
  const statusTone = toStatusTone(documentStatus?.status);
  const canAskQuestion = Boolean(documentId && documentStatus?.status === "COMPLETED");
  const processingErrorMessage = normalizeProcessingErrorMessage(documentStatus?.errorMessage);
  const hasFailedDocument = documentStatus?.status === "FAILED";

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

  const scrollToStepTwo = ({ focusQuestion = false } = {}) => {
    window.requestAnimationFrame(() => {
      if (stepTwoRef.current) {
        stepTwoRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
        if (focusQuestion) {
          window.setTimeout(() => {
            questionInputRef.current?.focus();
          }, 350);
        }
        return;
      }
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  };

  const resetSelectedFile = () => {
    setFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const chooseAnotherFile = () => {
    setMessage("");
    setError("");
    setDocumentStatus(null);
    setDocumentId("");
    setActiveStep(1);
    resetSelectedFile();
    window.requestAnimationFrame(() => {
      fileInputRef.current?.click();
    });
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

  useEffect(() => {
    if (canAskQuestion) {
      setActiveStep(2);
    }
  }, [canAskQuestion]);

  useEffect(() => {
    if (!shouldScrollToStepTwoRef.current || activeStep !== 2) {
      return;
    }

    shouldScrollToStepTwoRef.current = false;
    scrollToStepTwo();
  }, [activeStep, documentStatus]);

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

    if (!String(file.name || "").toLowerCase().endsWith(".pdf")) {
      resetSelectedFile();
      setError(`${supportedFileMessage} Please choose a .pdf file.`);
      return;
    }

    if ((file.size || 0) > MAX_UPLOAD_SIZE_BYTES) {
      resetSelectedFile();
      setError(`“${file.name || "This file"}” is too large. Please choose a file under ${MAX_UPLOAD_SIZE_MB} MB.`);
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    const requestId = ++uploadRequestIdRef.current;

    try {
      setIsUploading(true);
      shouldScrollToStepTwoRef.current = true;
      setActiveStep(2);
      setDocumentStatus({
        documentId: null,
        fileName: file.name || "document",
        status: "UPLOADING",
        chunksStored: 0,
        completedAt: null,
        errorMessage: null
      });
      setMessage(`Uploading ${file.name || "your document"} now. We will keep the live processing status updated here.`);
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
          chunksStored: data?.chunksStored ?? 0,
          completedAt: null,
          errorMessage: null
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
      const detail = normalizeUploadErrorMessage(statusCode, apiError || err.message, file?.name);
      shouldScrollToStepTwoRef.current = true;
      setActiveStep(2);
      setDocumentStatus({
        documentId: null,
        fileName: file?.name || "document",
        status: "FAILED",
        chunksStored: 0,
        completedAt: null,
        errorMessage: detail
      });
      setError(statusCode ? `We could not upload that file (${statusCode}). ${detail}` : detail);
    } finally {
      if (requestId === uploadRequestIdRef.current) {
        setIsUploading(false);
      }
      resetSelectedFile();
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
      <section ref={workspaceTopRef} className="card card-wide">
        <BrandBar />
        <div className="workspace-hero">
          <div>
            <p className="eyebrow">Review Workspace</p>
            <h1>Upload, track, and review with confidence</h1>
            <p className="muted workspace-lead">
              Bring in a document, follow its processing status, and ask focused questions once the review is ready.
            </p>
          </div>
          <div className="workspace-summary">\n            <span className={`status-pill status-${statusTone}`}>{statusLabel}</span>\n            <p>{canAskQuestion ? "Your document is ready for questions." : "Upload a file to begin the review flow."}</p>\n            <div className="workspace-summary-actions">\n              <Link className="summary-link" to="/policy-studio">Open Policy Studio</Link>\n            </div>\n          </div>
        </div>

        <div className="workspace-grid">
          <section className="workspace-main">
            <div className={`workspace-panel step-panel ${activeStep === 1 ? "step-panel-active" : ""}`}>
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Step 1</p>
                  <h2>Upload a document</h2>
                </div>
                <span className="panel-note">PDF files only</span>
              </div>
              <p className="muted">
                Choose a PDF and we will start processing it in the background so you can track progress right away.
              </p>

              <form onSubmit={handleSubmit} className="form">
                <label htmlFor="file">Document file</label>
                <input
                  ref={fileInputRef}
                  id="file"
                  type="file"
                  accept=".pdf,application/pdf"
                  onChange={(e) => {
                    setError("");
                    setMessage("");
                    setDocumentStatus(null);
                    setFile(e.target.files?.[0] || null);
                  }}
                />
                <p className="muted">
                  {supportedFileMessage} Upload limit: {MAX_UPLOAD_SIZE_MB} MB.
                </p>

                <button type="submit" disabled={isUploading || !file}>
                  {isUploading ? "Uploading..." : "Upload Document"}
                </button>
                {(error || hasFailedDocument) && (
                  <button type="button" className="secondary subtle-button" onClick={chooseAnotherFile}>
                    Choose Another File
                  </button>
                )}
              </form>
            </div>

            <div ref={stepTwoRef} className={`workspace-panel step-panel ${activeStep === 2 ? "step-panel-active" : ""}`}>
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Step 2</p>
                  <h2>Ask a question</h2>
                </div>
                <span className="panel-note">
                  {documentStatus
                    ? canAskQuestion
                      ? "Ready for questions"
                      : `Status: ${statusLabel}`
                    : "Ready after processing completes"}
                </span>
              </div>
              <p className="muted">
                Ask about risks, responsibilities, exclusions, or any other important part of the document.
              </p>

              {!!documentStatus && (
                <div className={`step-status-card step-status-${statusTone}`}>
                  <div className="step-status-head">
                    <strong>{documentStatus.fileName || file?.name || "Document"}</strong>
                    <span className={`status-pill status-${statusTone}`}>{statusLabel}</span>
                  </div>
                  <p>
                    {documentStatus.documentId
                      ? `Document ID ${documentStatus.documentId} is ${statusLabel.toLowerCase()}.`
                      : hasFailedDocument
                        ? "This file could not be uploaded successfully. Please remove it and choose another file."
                        : "We are uploading your file and will attach the document ID as soon as the server responds."}
                  </p>
                  {hasFailedDocument && (
                    <button type="button" className="secondary subtle-button" onClick={chooseAnotherFile}>
                      Remove File And Try Another
                    </button>
                  )}
                </div>
              )}

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
                  ref={questionInputRef}
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
              {processingErrorMessage && (
                <p className="error"><strong>Processing Error:</strong> {processingErrorMessage}</p>
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
            {processingErrorMessage && <p className="error"><strong>Processing Error:</strong> {processingErrorMessage}</p>}
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
                    <p className="eyebrow">Analyst {idx + 1} Opinion</p>
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

