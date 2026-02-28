import { Link } from "react-router-dom";

export default function ErrorPage() {
  return (
    <main className="page">
      <section className="card">
        <h1>Something Went Wrong</h1>
        <p>The page you requested is unavailable or an unexpected error occurred.</p>
        <div className="actions">
          <Link to="/">Back to Login</Link>
          <Link to="/upload">Try Upload Page</Link>
        </div>
      </section>
    </main>
  );
}
