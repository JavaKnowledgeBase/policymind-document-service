import { Link } from "react-router-dom";
import BrandBar from "../components/BrandBar";
import DeveloperCredit from "../components/DeveloperCredit";

export default function ErrorPage() {
  return (
    <main className="page">
      <section className="card card-wide">
        <BrandBar />
        <p className="eyebrow">PolicyMind Reliability</p>
        <h1>We Could Not Complete This Request</h1>
        <p className="muted">
          The platform is designed for graceful degradation with observability, retry logic, and clear error
          surfaces so teams can recover quickly.
        </p>

        <ul className="feature-list">
          <li>Check backend health and logs for service-level issues.</li>
          <li>Validate auth token/session and retry the operation.</li>
          <li>Return to upload workflow once connectivity is restored.</li>
        </ul>

        <div className="actions">
          <Link to="/">Back to Login</Link>
          <Link to="/about">Architecture Overview</Link>
          <Link to="/upload">Try Upload Page</Link>
        </div>
      </section>
      <DeveloperCredit />
    </main>
  );
}
