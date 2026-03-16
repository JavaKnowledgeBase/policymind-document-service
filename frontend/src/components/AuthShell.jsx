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
            <h1>Review complex documents with more clarity</h1>
            <p>
              A simple, secure way to upload documents, ask questions, and get clear answers fast.
            </p>
            <ul className="feature-list">
              <li>Turns dense policy language into plain-English summaries.</li>
              <li>Brings key risks, important clauses, and next steps to the surface.</li>
              <li>Keeps your team moving with a guided and secure review workflow.</li>
            </ul>
            <div className="highlight-strip">
              <div className="highlight-card">
                <strong>Faster reviews</strong>
                <span>Spend less time searching through long documents.</span>
              </div>
              <div className="highlight-card">
                <strong>Clear answers</strong>
                <span>Ask a question and see the evidence behind the response.</span>
              </div>
            </div>
          </article>

          <article className="login-panel">
            <h2>{title}</h2>
            <p className="muted">{subtitle}</p>
            {children}
          </article>
        </div>
      </section>
      <div className="login-bottom-left">
        <p>Clear answers</p>
        <p>Secure sign-in</p>
        <p>Reliable search</p>
      </div>
      <DeveloperCredit />
    </main>
  );
}
