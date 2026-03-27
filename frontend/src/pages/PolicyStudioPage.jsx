import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
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
  const navigate = useNavigate();

  const isRewrite = mode === "rewrite";
  const graphNodes = toGraphNodes(composeResult?.graphWorkflow || savedDrafts.find((item) => item.draftId === currentDraftId)?.graphWorkflow);
  const draftStats = useMemo(() => {
    const text = String(workingDraft || "");
    return {
      characters: text.length,
      words: text.trim() ? text.trim().split(/\s+/).length : 0
    };
  }, [workingDraft]);

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

  useEffect(() => {
    loadDrafts();
  }, []);

  useEffect(() => {
    if (mode === "create") {
      setWorkingDraft("");
      setComposeResult(null);
      setMessage("A blank draft workspace is ready. Add your requirements, then generate a first draft when you are ready.");
      setError("");
    }
  }, [mode]);

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
      ? "Paste the current policy first. We will copy it into a new working draft so you can update without touching the original."
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
        mustIncludeClauses: joinLines(draft.keyChanges),
        prohibitedClauses: joinLines(draft.riskFlags)
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
            <p>Paste the current policy, create a new working copy, and let PolicyMind rewrite it into a stronger publish-ready draft.</p>
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
                    <textarea id="sourceText" rows={10} value={form.sourceText} onChange={(e) => handleFieldChange("sourceText", e.target.value)} placeholder="Paste the current policy here. This remains the original source while we create a new working draft below." />
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
              <textarea className="policy-editor" value={workingDraft} onChange={(e) => setWorkingDraft(e.target.value)} placeholder={isRewrite ? "Paste the original policy above, then copy it into this working draft." : "Start your policy here or use the generator to create the first version."} rows={22} />
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
                    <span>v{draft.currentVersionNumber || 1} · {draft.latestQualityScore ?? 0} score</span>
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
