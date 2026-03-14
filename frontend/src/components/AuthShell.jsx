import BrandBar from "./BrandBar";
import DeveloperCredit from "./DeveloperCredit";

export default function AuthShell({ title, subtitle, children }) {
  return (
    <main className="page">
      <section className="card card-wide">
        <BrandBar />
        <div className="vision-shell">
          <article className="vision-panel">
            <p className="eyebrow">PolicyMind AI</p>
            <h1>Understand Complex Policies and Contracts in Minutes</h1>
            <p>
              AI-powered policy intelligence platform built on microservices, RAG retrieval, vector search,
              and secure cloud-ready architecture.
            </p>
            <ul className="feature-list">
              <li>Simplifies legal and insurance language for faster decisions.</li>
              <li>Highlights risk areas and clause-level insights for each document.</li>
              <li>Supports growth from freemium users to enterprise agency workflows.</li>
            </ul>
          </article>

          <article className="login-panel">
            <h2>{title}</h2>
            <p className="muted">{subtitle}</p>
            {children}
          </article>
        </div>
      </section>
      <div className="login-bottom-left">
        <p>RAG + Explainability</p>
        <p>JWT + Secure APIs</p>
        <p>Redis + Vector Search</p>
      </div>
      <DeveloperCredit />
    </main>
  );
}
