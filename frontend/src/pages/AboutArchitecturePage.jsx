import { Link } from "react-router-dom";
import BrandBar from "../components/BrandBar";
import DeveloperCredit from "../components/DeveloperCredit";

export default function AboutArchitecturePage() {
  return (
    <main className="page">
      <section className="card card-wide">
        <BrandBar />
        <p className="eyebrow">System Design</p>
        <h1>PolicyMind AI Architecture Overview</h1>
        <p className="muted">
          PolicyMind AI is a cloud-native document intelligence platform combining secure APIs, RAG retrieval,
          vector search, and caching to deliver explainable policy and contract insights.
        </p>

        <div className="arch-grid">
          <div className="arch-node">Frontend (React)</div>
          <div className="arch-arrow">to</div>
          <div className="arch-node">API / Auth (Spring + JWT)</div>
          <div className="arch-arrow">to</div>
          <div className="arch-node">Document Service</div>
          <div className="arch-arrow">to</div>
          <div className="arch-node">Embedding + Vector Store</div>
          <div className="arch-arrow">to</div>
          <div className="arch-node">AI Orchestrator (RAG)</div>
        </div>

        <div className="metrics-row">
          <div className="metric-card">
            <strong>Core Flow</strong>
            <span>Upload to Chunk to Embed to Retrieve to Explain</span>
          </div>
          <div className="metric-card">
            <strong>Performance</strong>
            <span>Redis caching reduces repeated LLM calls and latency.</span>
          </div>
          <div className="metric-card">
            <strong>Scalability</strong>
            <span>Dockerized services with independently deployable boundaries.</span>
          </div>
        </div>

        <h2>Interview Speaker Notes</h2>
        <ol className="notes-list">
          <li>Start with business value: simplify policy/legal complexity into actionable decisions.</li>
          <li>Explain architecture: frontend, auth, document processing, retrieval, and orchestration layers.</li>
          <li>Walk through upload flow: parse document, chunk text, generate embeddings, index context.</li>
          <li>Walk through query flow: retrieve top context, build prompt, return structured explanation.</li>
          <li>Call out production readiness: JWT, CORS, logs, Docker deployment, and service isolation.</li>
          <li>Close with growth path: freemium to enterprise, API licensing, and compliance automation.</li>
        </ol>

        <div className="actions">
          <Link to="/">Back to Login</Link>
          <Link to="/upload">Go to Upload</Link>
          <Link to="/error">View Reliability Page</Link>
        </div>
      </section>
      <DeveloperCredit />
    </main>
  );
}
