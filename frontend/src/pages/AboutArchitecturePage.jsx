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
          PolicyMind AI helps teams review policies and contracts faster with secure uploads, smart search,
          and easy-to-follow answers.
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
            <span>Smart caching helps repeat requests return faster.</span>
          </div>
          <div className="metric-card">
            <strong>Scalability</strong>
            <span>Built in separate services so it can grow with your team.</span>
          </div>
        </div>

        <h2>System Architecture</h2>
        <ul className="notes-list">
          <li><strong>Frontend Layer:</strong> React powers the secure user workspace for authentication, document upload, status tracking, and guided question-answer workflows.</li>
          <li><strong>API and Auth Layer:</strong> Spring Boot services expose protected APIs with JWT-based authentication and clear service boundaries for user access, document operations, and AI requests.</li>
          <li><strong>Microservices-Oriented Processing:</strong> The platform is structured in separate backend responsibilities for upload handling, parsing, chunking, embedding, retrieval, and orchestration so the system can scale cleanly over time.</li>
          <li><strong>Document Ingestion Pipeline:</strong> Uploaded files move through text extraction, validation, chunk generation, embedding creation, persistence, and asynchronous background processing.</li>
          <li><strong>RAG Orchestration:</strong> PolicyMind uses Retrieval-Augmented Generation to combine user questions with retrieved document context and return structured, evidence-backed responses.</li>
          <li><strong>Vector Search and Retrieval:</strong> Document chunks are embedded and ranked semantically at query time so the application can surface the most relevant context instead of relying on keyword matching alone.</li>
          <li><strong>Caching and Background Workflows:</strong> Redis-backed caching and async processing reduce repeated work, improve responsiveness, and support reliable document status polling.</li>
          <li><strong>Data Layer:</strong> PostgreSQL stores document metadata, chunk records, embeddings, processing state, and analysis outputs for traceability and repeatable retrieval.</li>
          <li><strong>Deployment Architecture:</strong> Containerized services, reverse proxy support, health checks, and deployment automation make the platform reliable in both local and cloud-ready environments.</li>
        </ul>

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
