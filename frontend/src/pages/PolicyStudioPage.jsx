import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import client from "../api/client";
import BrandBar from "../components/BrandBar";

const EMPTY_FORM = {
  provider: "openai",
  policyType: "",
  title: "",
  jurisdiction: "United States",
  audience: "Employees and managers",
  tone: "Clear, practical, and professional",
  goals: "",
  sourceText: "",
  additionalInstructions: "",
  mustIncludeClauses: "",
  prohibitedClauses: ""
};
const MAX_UPLOAD_SIZE_MB = 50;
const MAX_UPLOAD_SIZE_BYTES = MAX_UPLOAD_SIZE_MB * 1024 * 1024;

function splitLines(value) {
  return String(value || "")
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function joinLines(value) {
  return Array.isArray(value) ? value.join("\n") : "";
}

function toGraphNodes(graphWorkflow) {
  return Array.isArray(graphWorkflow?.nodes) ? graphWorkflow.nodes : [];
}

function toDisplayList(items) {
  return Array.isArray(items) ? items.filter(Boolean) : [];
}

function toPolicyStudioLines(items, fallbackField) {
  return toDisplayList(items)
    .map((item) => {
      if (typeof item === "string") {
        return item;
      }
      if (item && typeof item === "object") {
        return item.text || item.recommendedText || item.suggestedText || item.content || item.reason || item.title || item.name || item.clauseType || item[fallbackField] || JSON.stringify(item);
      }
      return "";
    })
    .filter(Boolean)
    .join("\n");
}

function toStatusTone(status) {
  if (status === "COMPLETED") {
    return "success";
  }
  if (status === "FAILED") {
    return "danger";
  }
  if (status === "PROCESSING" || status === "QUEUED" || status === "UPLOADING") {
    return "warning";
  }
  return "neutral";
}

function normalizeUploadErrorMessage(statusCode, message, fileName) {
  if (statusCode === 413) {
    return `"${fileName || "This file"}" is larger than the current upload limit. Please choose a file under ${MAX_UPLOAD_SIZE_MB} MB.`;
  }

  if (String(message || "").includes("Only PDF files are supported right now.")) {
    return "Only PDF files are supported right now. Please choose a .pdf file.";
  }

  return message || "The upload did not finish successfully.";
}

export default function PolicyStudioPage() {
  const [mode, setMode] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [workingDraft, setWorkingDraft] = useState("");
  const [composeResult, setComposeResult] = useState(null);
  const [currentDraftId, setCurrentDraftId] = useState(null);
  const [savedDrafts, setSavedDrafts] = useState([]);
  const [versionHistory, setVersionHistory] = useState([]);
  const [isComposing, setIsComposing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isLoadingDrafts, setIsLoadingDrafts] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [sourceMethod, setSourceMethod] = useState("paste");
  const [uploadFile, setUploadFile] = useState(null);
  const [uploadedDocumentId, setUploadedDocumentId] = useState("");
  const [uploadedDocumentStatus, setUploadedDocumentStatus] = useState(null);
  const [reviewData, setReviewData] = useState(null);
  const [isUploadingPolicy, setIsUploadingPolicy] = useState(false);
  const [isReviewingPolicy, setIsReviewingPolicy] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const uploadRequestIdRef = useRef(0);
  const statusRequestIdRef = useRef(0);
  const fileInputRef = useRef(null);

  const isRewrite = mode === "rewrite";
  const graphNodes = toGraphNodes(composeResult?.graphWorkflow || savedDrafts.find((item) => item.draftId === currentDraftId)?.graphWorkflow);
  const draftStats = useMemo(() => {
    const text = String(workingDraft || "");
    return {
      characters: text.length,
      words: text.trim() ? text.trim().split(/\s+/).length : 0
    };
  }, [workingDraft]);
  const reviewSummary = reviewData?.summary || null;
  const missingClauses = toDisplayList(reviewData?.missingClauses);
  const riskyClauses = toDisplayList(reviewData?.riskyClauses);
  const suggestedClauses = toDisplayList(reviewData?.suggestedClauses);
  const canRunIntegratedReview = Boolean(uploadedDocumentId && uploadedDocumentStatus?.status === "COMPLETED");
  const integratedStatusTone = toStatusTone(uploadedDocumentStatus?.status);
  const importedReviewContext = reviewData?.reviewContext || null;

  const loadDrafts = async () => {
    try {
      setIsLoadingDrafts(true);
      const response = await client.get("/policy-drafts");
      setSavedDrafts(response.data || []);
    } catch {
      setError((current) => current || "We could not load saved drafts right now.");
    } finally {
      setIsLoadingDrafts(false);
    }
  };

  const loadVersions = async (draftId) => {
    if (!draftId) {
      setVersionHistory([]);
      return;
    }
    try {
      const response = await client.get(`/policy-drafts/${draftId}/versions`);
      setVersionHistory(response.data || []);
    } catch {
      setVersionHistory([]);
    }
  };

  const applyReviewToIntake = (payload, options = {}) => {
    if (!payload) {
      return;
    }

    const nextMissingClauses = toDisplayList(payload.missingClauses);
    const nextRiskyClauses = toDisplayList(payload.riskyClauses);
    const nextSuggestedClauses = toDisplayList(payload.suggestedClauses);
    const nextReferenceSources = toDisplayList(payload.referenceSources);
    const nextSummary = payload.summary || {};
    const nextSourceText = nextSummary.documentText || "";
    const nextGoals = [
      "Rewrite this HR/internal policy using the uploaded policy as the source draft.",
      nextMissingClauses.length ? `Address ${nextMissingClauses.length} missing clauses identified during review.` : "",
      nextRiskyClauses.length ? `Reduce or replace ${nextRiskyClauses.length} risky clauses identified during review.` : "",
      nextSuggestedClauses.length ? "Incorporate trusted replacement language where appropriate." : ""
    ]
      .filter(Boolean)
      .join(" ");
    const nextInstructions = [
      nextSummary.assessment ? `Assessment: ${nextSummary.assessment}` : "",
      nextSummary.overview ? `Overview: ${nextSummary.overview}` : "",
      nextReferenceSources.length ? `Reference sources: ${nextReferenceSources.map((item) => item.sourceName || item.title || item).join(", ")}` : ""
    ]
      .filter(Boolean)
      .join("\n\n");

    setMode("rewrite");
    setSourceMethod("upload");
    setReviewData({
      ...payload,
      reviewContext: {
        documentId: options.documentId || uploadedDocumentId || payload.documentId,
        fileName: options.fileName || uploadedDocumentStatus?.fileName || uploadFile?.name || payload.fileName || "Uploaded policy",
        missingClauses: nextMissingClauses,
        riskyClauses: nextRiskyClauses,
        suggestedClauses: nextSuggestedClauses,
        referenceSources: nextReferenceSources
      }
    });
    setForm((current) => ({
      ...current,
      policyType: payload.policyType || nextSummary.policyType || current.policyType,
      goals: nextGoals || current.goals,
      sourceText: nextSourceText || current.sourceText,
      additionalInstructions: nextInstructions || current.additionalInstructions,
      mustIncludeClauses: toPolicyStudioLines([...nextMissingClauses, ...nextSuggestedClauses], "recommendedText"),
      prohibitedClauses: toPolicyStudioLines(nextRiskyClauses, "content")
    }));
    setWorkingDraft(nextSourceText || "");
    setComposeResult(null);
    setMessage("Review findings were loaded directly into Policy Studio. You can refine the intake, then rewrite the policy without switching workspaces.");
    setError("");
  };

  useEffect(() => {
    loadDrafts();
  }, []);

  useEffect(() => {
    if (!location.state) {
      return;
    }

    applyReviewToIntake(location.state, {
      documentId: location.state.reviewContext?.documentId,
      fileName: location.state.reviewContext?.fileName
    });
    navigate(location.pathname, { replace: true, state: null });
  }, [location.state]);

  useEffect(() => {
    if (mode === "create") {
      setSourceMethod("paste");
      setWorkingDraft("");
      setComposeResult(null);
      setReviewData(null);
      setUploadedDocumentStatus(null);
      setUploadedDocumentId("");
      setUploadFile(null);
      setMessage("A blank draft workspace is ready. Add your requirements, then generate a first draft when you are ready.");
      setError("");
    }
  }, [mode]);

  useEffect(() => {
    if (!uploadedDocumentId || !uploadedDocumentStatus || !["QUEUED", "PROCESSING"].includes(uploadedDocumentStatus.status)) {
      return undefined;
    }

    const intervalId = window.setInterval(async () => {
      try {
        const requestId = ++statusRequestIdRef.current;
        const response = await client.get(`/documents/${uploadedDocumentId}`);
        if (requestId !== statusRequestIdRef.current) {
          return;
        }
        setUploadedDocumentStatus(response.data);
      } catch {
        // Keep the last known status visible.
      }
    }, 3000);

    return () => window.clearInterval(intervalId);
  }, [uploadedDocumentId, uploadedDocumentStatus]);

  const handleLogout = () => {
    localStorage.removeItem("authToken");
    navigate("/");
  };

  const handleModeSelect = (nextMode) => {
    setMode(nextMode);
    setComposeResult(null);
    setCurrentDraftId(null);
    setVersionHistory([]);
    setError("");
    setMessage(nextMode === "rewrite"
      ? "Choose how you want to bring the current policy in. You can paste text or upload a PDF for review without leaving Policy Studio."
      : "Start from scratch selected. Your new policy draft will begin on a blank page.");
    if (nextMode === "rewrite") {
      setWorkingDraft(form.sourceText || "");
    }
  };

  const handleFieldChange = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const loadSourceIntoDraft = () => {
    if (!form.sourceText.trim()) {
      setError("Paste the existing policy text first so we can create a new working draft from it.");
      return;
    }
    setWorkingDraft(form.sourceText);
    setComposeResult(null);
    setMessage("The existing policy has been copied into a new working draft. You can edit here before or after generation.");
    setError("");
  };

  const handlePolicyUpload = async () => {
    setError("");
    setMessage("");
    setReviewData(null);

    if (!uploadFile) {
      setError("Choose a PDF policy file to review first.");
      return;
    }

    if (!String(uploadFile.name || "").toLowerCase().endsWith(".pdf")) {
      setError("Only PDF files are supported right now. Please choose a .pdf file.");
      return;
    }

    if ((uploadFile.size || 0) > MAX_UPLOAD_SIZE_BYTES) {
      setError(`"${uploadFile.name || "This file"}" is too large. Please choose a file under ${MAX_UPLOAD_SIZE_MB} MB.`);
      return;
    }

    const formData = new FormData();
    formData.append("file", uploadFile);
    const requestId = ++uploadRequestIdRef.current;

    try {
      setIsUploadingPolicy(true);
      setUploadedDocumentStatus({
        documentId: null,
        fileName: uploadFile.name || "document",
        status: "UPLOADING",
        chunksStored: 0,
        completedAt: null,
        errorMessage: null
      });
      const response = await client.post("/upload", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
          "X-Upload-Request-Id": `studio-upload-${Date.now()}-${requestId}`
        }
      });
      if (requestId !== uploadRequestIdRef.current) {
        return;
      }
      const data = response.data || {};
      const nextDocumentId = data?.documentId ? String(data.documentId) : "";
      setUploadedDocumentId(nextDocumentId);
      setUploadedDocumentStatus({
        documentId: data?.documentId ?? null,
        fileName: data?.fileName || uploadFile.name || "document",
        status: data?.status || "QUEUED",
        chunksStored: data?.chunksStored ?? 0,
        completedAt: null,
        errorMessage: null
      });
      setMessage(`${data?.fileName || uploadFile.name || "Document"} is in the review pipeline. Once processing finishes, you can load the findings straight into the intake below.`);
    } catch (err) {
      if (requestId !== uploadRequestIdRef.current) {
        return;
      }
      const detail = normalizeUploadErrorMessage(err.response?.status, err.response?.data?.error || err.message, uploadFile?.name);
      setUploadedDocumentStatus({
        documentId: null,
        fileName: uploadFile?.name || "document",
        status: "FAILED",
        chunksStored: 0,
        completedAt: null,
        errorMessage: detail
      });
      setError(detail);
    } finally {
      if (requestId === uploadRequestIdRef.current) {
        setIsUploadingPolicy(false);
      }
    }
  };

  const handleIntegratedReview = async () => {
    setError("");
    setMessage("");

    if (!canRunIntegratedReview) {
      setError("Finish processing the uploaded policy before running review.");
      return;
    }

    try {
      setIsReviewingPolicy(true);
      const response = await client.get(`/documents/${uploadedDocumentId}/review`);
      applyReviewToIntake(response.data, {
        documentId: uploadedDocumentId,
        fileName: uploadedDocumentStatus?.fileName
      });
    } catch (err) {
      setError(err.response?.data?.error || "We could not review that policy right now. Please try again in a moment.");
    } finally {
      setIsReviewingPolicy(false);
    }
  };

  const resetIntegratedUpload = () => {
    setUploadFile(null);
    setUploadedDocumentId("");
    setUploadedDocumentStatus(null);
    setReviewData(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleCompose = async () => {
    setError("");
    setMessage("");
    setComposeResult(null);

    if (!mode) {
      setError("Choose Update Existing or Start New before composing.");
      return;
    }

    if (!form.policyType.trim()) {
      setError("Add a policy type so the draft has a clear scope.");
      return;
    }

    if (!form.goals.trim()) {
      setError("Describe the business goal before generating a draft.");
      return;
    }

    if (isRewrite && !workingDraft.trim()) {
      setError("Update mode needs an existing policy in the working draft.");
      return;
    }

    try {
      setIsComposing(true);
      const response = await client.post("/policy-compose", {
        mode,
        provider: form.provider,
        policyType: form.policyType,
        title: form.title,
        jurisdiction: form.jurisdiction,
        audience: form.audience,
        tone: form.tone,
        goals: form.goals,
        sourceText: workingDraft,
        additionalInstructions: form.additionalInstructions,
        mustIncludeClauses: splitLines(form.mustIncludeClauses),
        prohibitedClauses: splitLines(form.prohibitedClauses)
      }, {
        headers: {
          "X-Policy-Compose-Request-Id": `compose-${Date.now()}`
        }
      });

      setComposeResult(response.data);
      setWorkingDraft(response.data?.draft || workingDraft);
      setMessage(
        isRewrite
          ? "Your updated draft is ready in the working editor below. The original source remains preserved in the intake section."
          : "Your new draft is ready in the working editor below."
      );
    } catch (err) {
      setError(err.response?.data?.error || "We could not generate the policy draft right now. Please try again.");
    } finally {
      setIsComposing(false);
    }
  };

  const saveDraft = async () => {
    setError("");
    setMessage("");

    if (!form.policyType.trim()) {
      setError("Add a policy type before saving a draft.");
      return;
    }

    if (!workingDraft.trim()) {
      setError("There is no working draft to save yet.");
      return;
    }

    try {
      setIsSaving(true);
      const response = await client.post("/policy-drafts", {
        draftId: currentDraftId,
        mode: mode || "create",
        provider: form.provider,
        policyType: form.policyType,
        title: form.title,
        jurisdiction: form.jurisdiction,
        audience: form.audience,
        tone: form.tone,
        goals: form.goals,
        additionalInstructions: form.additionalInstructions,
        sourceText: form.sourceText,
        workingDraft,
        mustIncludeClauses: splitLines(form.mustIncludeClauses),
        prohibitedClauses: splitLines(form.prohibitedClauses),
        summary: composeResult?.summary || "",
        rationale: composeResult?.rationale || "",
        confidence: composeResult?.confidence || "medium",
        qualityScore: composeResult?.qualityScore || 0,
        keyChanges: composeResult?.keyChanges || [],
        implementationChecklist: composeResult?.implementationChecklist || [],
        riskFlags: composeResult?.riskFlags || [],
        composeResult: composeResult || {}
      });

      const saved = response.data;
      setCurrentDraftId(saved.draftId);
      setComposeResult((current) => ({ ...(current || {}), ...saved }));
      setMessage(`Draft saved as version ${saved.currentVersionNumber}.`);
      await loadDrafts();
      await loadVersions(saved.draftId);
    } catch (err) {
      setError(err.response?.data?.error || "We could not save this draft right now.");
    } finally {
      setIsSaving(false);
    }
  };

  const openDraft = async (draftId) => {
    try {
      setError("");
      const response = await client.get(`/policy-drafts/${draftId}`);
      const draft = response.data;
      setCurrentDraftId(draft.draftId);
      setMode(draft.mode || "create");
      setSourceMethod("paste");
      setReviewData(null);
      setUploadedDocumentStatus(null);
      setUploadedDocumentId("");
      setUploadFile(null);
      setForm({
        provider: draft.provider || "openai",
        policyType: draft.policyType || "",
        title: draft.title || "",
        jurisdiction: draft.jurisdiction || "United States",
        audience: draft.audience || "Employees and managers",
        tone: draft.tone || "Clear, practical, and professional",
        goals: draft.goals || "",
        sourceText: draft.sourceText || "",
        additionalInstructions: draft.additionalInstructions || "",
        mustIncludeClauses: joinLines(draft.mustIncludeClauses),
        prohibitedClauses: joinLines(draft.prohibitedClauses)
      });
      setWorkingDraft(draft.workingDraft || "");
      setComposeResult(draft);
      setMessage(`Loaded saved draft \"${draft.title || draft.policyType}\".`);
      await loadVersions(draftId);
    } catch {
      setError("We could not open that saved draft.");
    }
  };

  const loadVersionIntoEditor = (version) => {
    setWorkingDraft(version.workingDraft || "");
    setComposeResult((current) => ({ ...(current || {}), ...version }));
    setMessage(`Loaded version ${version.versionNumber} into the editor.`);
    setError("");
  };

  const copyDraft = async () => {
    try {
      await navigator.clipboard.writeText(workingDraft || "");
      setMessage("The working draft has been copied to your clipboard.");
      setError("");
    } catch {
      setError("Clipboard access was not available in this browser.");
    }
  };

  return (
    <main className="page">
      <div className="page-orb page-orb-left" aria-hidden="true" />
      <div className="page-orb page-orb-right" aria-hidden="true" />
      <section className="card card-wide policy-studio-card">
        <BrandBar />
        <div className="workspace-topbar">
          <div>
            <p className="eyebrow">Compose Workspace</p>
            <h1>Policy Studio</h1>
            <p className="muted workspace-lead">
              Start with the right intake, open a real drafting canvas, generate stronger policy text, and keep versioned drafts you can reopen later.
            </p>
          </div>
          <div className="workspace-nav-actions">
            <Link className="secondary nav-link-button" to="/upload">Review Workspace</Link>
            <button type="button" className="secondary" onClick={handleLogout}>Log Out</button>
          </div>
        </div>

        <section className="mode-selector-grid">
          <button type="button" className={`mode-card ${mode === "rewrite" ? "mode-card-active" : ""}`} onClick={() => handleModeSelect("rewrite")}>
            <span className="panel-note">Update Existing</span>
            <h2>Rewrite and improve an existing policy</h2>
            <p>Bring in an existing policy by paste or PDF upload, then let PolicyMind turn it into a stronger publish-ready draft.</p>
          </button>
          <button type="button" className={`mode-card ${mode === "create" ? "mode-card-active" : ""}`} onClick={() => handleModeSelect("create")}>
            <span className="panel-note">Start New</span>
            <h2>Create a policy from scratch</h2>
            <p>Start on a blank page, capture business requirements first, then generate a full draft with rollout guidance and risk flags.</p>
          </button>
        </section>

        <div className="workspace-grid policy-studio-grid">
          <section className="workspace-main">
            <div className="workspace-panel">
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Step 1</p>
                  <h2>Intake</h2>
                </div>
                <span className="panel-note">Required before drafting</span>
              </div>
              <div className="form policy-intake-form">
                {importedReviewContext && (
                  <div className="step-status-card step-status-warning">
                    <div className="step-status-head">
                      <strong>{importedReviewContext.fileName || "Uploaded policy review"}</strong>
                      <span className="status-pill status-warning">Review Findings Loaded</span>
                    </div>
                    <p>
                      Document ID {importedReviewContext.documentId || "N/A"} brought in {importedReviewContext.missingClauses?.length || 0} missing clauses and {importedReviewContext.riskyClauses?.length || 0} risky clauses.
                    </p>
                  </div>
                )}

                {isRewrite && (
                  <div className="rewrite-source-shell">
                    <div className="rewrite-source-head">
                      <div>
                        <p className="eyebrow">Existing Policy Intake</p>
                        <h3>Choose how to bring the current policy in</h3>
                      </div>
                      <span className="panel-note">Stay in one workspace</span>
                    </div>
                    <div className="rewrite-source-toggle">
                      <button type="button" className={`source-method-chip ${sourceMethod === "paste" ? "source-method-chip-active" : ""}`} onClick={() => setSourceMethod("paste")}>Paste Policy Text</button>
                      <button type="button" className={`source-method-chip ${sourceMethod === "upload" ? "source-method-chip-active" : ""}`} onClick={() => setSourceMethod("upload")}>Upload PDF For Guided Review</button>
                    </div>

                    {sourceMethod === "upload" ? (
                      <div className="rewrite-upload-panel">
                        <label htmlFor="policyUpload">Existing policy PDF</label>
                        <input
                          ref={fileInputRef}
                          id="policyUpload"
                          type="file"
                          accept=".pdf,application/pdf"
                          onChange={(event) => {
                            setUploadFile(event.target.files?.[0] || null);
                            setError("");
                            setMessage("");
                          }}
                        />
                        <p className="muted">Upload a PDF, let PolicyMind review it, then pull the findings straight into this rewrite intake.</p>
                        <div className="rewrite-upload-actions">
                          <button type="button" onClick={handlePolicyUpload} disabled={isUploadingPolicy || !uploadFile}>
                            {isUploadingPolicy ? "Uploading..." : "Upload Policy"}
                          </button>
                          <button type="button" className="secondary" onClick={handleIntegratedReview} disabled={!canRunIntegratedReview || isReviewingPolicy}>
                            {isReviewingPolicy ? "Reviewing..." : "Run Review And Load Intake"}
                          </button>
                          <button type="button" className="secondary" onClick={resetIntegratedUpload} disabled={!uploadFile && !uploadedDocumentId && !reviewData}>
                            Clear Upload
                          </button>
                        </div>
                        {!!uploadedDocumentStatus && (
                          <div className={`step-status-card step-status-${integratedStatusTone}`}>
                            <div className="step-status-head">
                              <strong>{uploadedDocumentStatus.fileName || uploadFile?.name || "Uploaded policy"}</strong>
                              <span className={`status-pill status-${integratedStatusTone}`}>{uploadedDocumentStatus.status || "Waiting"}</span>
                            </div>
                            <p>
                              {uploadedDocumentStatus.documentId
                                ? `Document ID ${uploadedDocumentStatus.documentId} is ${String(uploadedDocumentStatus.status || "unknown").toLowerCase()}.`
                                : "Your file is being prepared for review."}
                            </p>
                          </div>
                        )}
                        {!!reviewData && (
                          <div className="review-summary-grid">
                            <div className="metric-card">
                              <strong>Assessment</strong>
                              <span>{reviewSummary?.assessment || "Ready"}</span>
                            </div>
                            <div className="metric-card">
                              <strong>Missing Clauses</strong>
                              <span>{missingClauses.length}</span>
                            </div>
                            <div className="metric-card">
                              <strong>Risky Clauses</strong>
                              <span>{riskyClauses.length}</span>
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="rewrite-paste-panel">
                        <p className="muted">Paste the current policy text directly when you already have it handy.</p>
                      </div>
                    )}
                  </div>
                )}

                <label htmlFor="policyType">Policy type</label>
                <input id="policyType" type="text" value={form.policyType} onChange={(e) => handleFieldChange("policyType", e.target.value)} placeholder="Example: Remote Work Policy" />

                <label htmlFor="title">Draft title</label>
                <input id="title" type="text" value={form.title} onChange={(e) => handleFieldChange("title", e.target.value)} placeholder="Optional working title" />

                <div className="policy-grid-2">
                  <div>
                    <label htmlFor="jurisdiction">Jurisdiction</label>
                    <input id="jurisdiction" type="text" value={form.jurisdiction} onChange={(e) => handleFieldChange("jurisdiction", e.target.value)} />
                  </div>
                  <div>
                    <label htmlFor="provider">AI provider</label>
                    <select id="provider" value={form.provider} onChange={(e) => handleFieldChange("provider", e.target.value)}>
                      <option value="openai">OpenAI</option>
                      <option value="vertex">Vertex</option>
                      <option value="both">Both</option>
                    </select>
                  </div>
                </div>

                <label htmlFor="audience">Audience</label>
                <input id="audience" type="text" value={form.audience} onChange={(e) => handleFieldChange("audience", e.target.value)} />

                <label htmlFor="tone">Tone</label>
                <input id="tone" type="text" value={form.tone} onChange={(e) => handleFieldChange("tone", e.target.value)} />

                <label htmlFor="goals">Business goals</label>
                <textarea id="goals" rows={4} value={form.goals} onChange={(e) => handleFieldChange("goals", e.target.value)} placeholder="Describe what this policy must achieve, who it protects, and what should change." />

                {isRewrite && (
                  <>
                    <label htmlFor="sourceText">Existing policy text</label>
                    <textarea id="sourceText" rows={10} value={form.sourceText} onChange={(e) => handleFieldChange("sourceText", e.target.value)} placeholder={sourceMethod === "upload" ? "Run a review above or paste edited source text here." : "Paste the current policy here. This remains the original source while we create a new working draft below."} />
                    <button type="button" className="secondary" onClick={loadSourceIntoDraft}>Copy Existing Policy Into New Draft</button>
                  </>
                )}

                <div className="policy-grid-2">
                  <div>
                    <label htmlFor="mustIncludeClauses">Must-include clauses</label>
                    <textarea id="mustIncludeClauses" rows={5} value={form.mustIncludeClauses} onChange={(e) => handleFieldChange("mustIncludeClauses", e.target.value)} placeholder="One clause or requirement per line" />
                  </div>
                  <div>
                    <label htmlFor="prohibitedClauses">Avoid or remove</label>
                    <textarea id="prohibitedClauses" rows={5} value={form.prohibitedClauses} onChange={(e) => handleFieldChange("prohibitedClauses", e.target.value)} placeholder="Language to avoid, remove, or de-emphasize" />
                  </div>
                </div>

                <label htmlFor="additionalInstructions">Additional instructions</label>
                <textarea id="additionalInstructions" rows={4} value={form.additionalInstructions} onChange={(e) => handleFieldChange("additionalInstructions", e.target.value)} placeholder="Optional drafting notes, compliance themes, rollout context, or redline directions" />
              </div>
            </div>

            <div className="workspace-panel">
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Step 2</p>
                  <h2>Working draft editor</h2>
                </div>
                <span className="panel-note">{isRewrite ? "New draft created from existing policy" : "Blank canvas"}</span>
              </div>
              <textarea className="policy-editor" value={workingDraft} onChange={(e) => setWorkingDraft(e.target.value)} placeholder={isRewrite ? "Bring the source policy into intake above, then copy it into this working draft." : "Start your policy here or use the generator to create the first version."} rows={22} />
              <div className="policy-editor-actions">
                <div className="draft-stats">
                  <span>{draftStats.words} words</span>
                  <span>{draftStats.characters} characters</span>
                  <span>{currentDraftId ? `Draft #${currentDraftId}` : "Unsaved draft"}</span>
                </div>
                <div className="policy-action-group">
                  <button type="button" className="secondary" onClick={copyDraft} disabled={!workingDraft.trim()}>Copy Draft</button>
                  <button type="button" className="secondary" onClick={saveDraft} disabled={isSaving || !workingDraft.trim()}>{isSaving ? "Saving..." : currentDraftId ? "Save New Version" : "Save Draft"}</button>
                  <button type="button" onClick={handleCompose} disabled={isComposing}>{isComposing ? "Generating..." : isRewrite ? "Rewrite This Draft" : "Generate First Draft"}</button>
                </div>
              </div>
            </div>

            {!!graphNodes.length && (
              <div className="workspace-panel">
                <div className="panel-head">
                  <div>
                    <p className="eyebrow">Graph Workflow</p>
                    <h2>Pipeline status</h2>
                  </div>
                </div>
                <div className="graph-workflow-grid">
                  {graphNodes.map((node) => (
                    <div key={node.key} className={`graph-node graph-node-${node.status}`}>
                      <strong>{String(node.key || "step").replace(/_/g, " ")}</strong>
                      <span>{node.status}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {composeResult && (
              <div className="workspace-panel">
                <div className="panel-head">
                  <div>
                    <p className="eyebrow">Step 3</p>
                    <h2>Draft intelligence</h2>
                  </div>
                  <span className="panel-note">Selected provider: {composeResult.selectedProvider || form.provider}</span>
                </div>
                <div className="rag-badges">
                  <span className="badge">Confidence: {composeResult.confidence || "medium"}</span>
                  <span className="badge">Quality Score: {composeResult.qualityScore ?? "N/A"}</span>
                  <span className="badge">Version: {composeResult.currentVersionNumber ?? versionHistory[0]?.versionNumber ?? "Not saved yet"}</span>
                </div>
                <div className="analysis-hero-card policy-summary-card">
                  <div>
                    <p className="analysis-label">Summary</p>
                    <p className="analysis-summary">{composeResult.summary || "No summary available."}</p>
                  </div>
                  <div>
                    <p className="analysis-label">Rationale</p>
                    <p className="analysis-answer">{composeResult.rationale || "No rationale available."}</p>
                  </div>
                </div>
                <div className="rag-grid">
                  <div className="rag-section insight-card">
                    <h3>Key changes</h3>
                    <ul>
                      {(composeResult.keyChanges || []).map((item, idx) => <li key={`change-${idx}`}>{item}</li>)}
                    </ul>
                  </div>
                  <div className="rag-section insight-card">
                    <h3>Implementation checklist</h3>
                    <ul>
                      {(composeResult.implementationChecklist || []).map((item, idx) => <li key={`check-${idx}`}>{item}</li>)}
                    </ul>
                  </div>
                </div>
                <div className="rag-section insight-card risk-panel">
                  <h3>Residual risk flags</h3>
                  {(composeResult.riskFlags || []).length ? (
                    <ul>
                      {(composeResult.riskFlags || []).map((item, idx) => <li key={`risk-${idx}`}>{item}</li>)}
                    </ul>
                  ) : (
                    <p>No major residual risks were surfaced in this generation run.</p>
                  )}
                </div>
              </div>
            )}
          </section>

          <aside className="workspace-sidebar">
            <div className="workspace-panel sidebar-panel">
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Saved Drafts</p>
                  <h2>Your workspace library</h2>
                </div>
                <button type="button" className="secondary subtle-button" onClick={loadDrafts}>{isLoadingDrafts ? "Refreshing..." : "Refresh"}</button>
              </div>
              <div className="draft-library">
                {!savedDrafts.length && <p className="muted">No saved drafts yet. Save your first working draft to start version history.</p>}
                {savedDrafts.map((draft) => (
                  <button key={draft.draftId} type="button" className={`draft-list-item ${draft.draftId === currentDraftId ? "draft-list-item-active" : ""}`} onClick={() => openDraft(draft.draftId)}>
                    <strong>{draft.title || draft.policyType}</strong>
                    <span>{draft.policyType}</span>
                    <span>v{draft.currentVersionNumber || 1} - {draft.latestQualityScore ?? 0} score</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="workspace-panel sidebar-panel">
              <div className="panel-head">
                <div>
                  <p className="eyebrow">Version History</p>
                  <h2>Checkpoint timeline</h2>
                </div>
              </div>
              <div className="version-history-list">
                {!versionHistory.length && <p className="muted">Open or save a draft to see version checkpoints.</p>}
                {versionHistory.map((version) => (
                  <button key={version.versionId} type="button" className="version-item" onClick={() => loadVersionIntoEditor(version)}>
                    <strong>Version {version.versionNumber}</strong>
                    <span>{version.confidence || "medium"} confidence</span>
                    <span>{version.qualityScore ?? 0} quality score</span>
                  </button>
                ))}
              </div>
            </div>
          </aside>
        </div>

        {message && <p className="success">{message}</p>}
        {error && <p className="error">{error}</p>}
      </section>
    </main>
  );
}

