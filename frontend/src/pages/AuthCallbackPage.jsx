import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import BrandBar from "../components/BrandBar";
import DeveloperCredit from "../components/DeveloperCredit";

export default function AuthCallbackPage() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get("token");
    const error = params.get("error");

    if (token) {
      localStorage.setItem("authToken", token);
      navigate("/upload", { replace: true });
      return;
    }

    if (error) {
      navigate("/?oauthError=1", { replace: true });
      return;
    }

    navigate("/", { replace: true });
  }, [location.search, navigate]);

  return (
    <main className="page">
      <section className="card">
        <BrandBar />
        <h1>Completing Sign-In</h1>
        <p className="muted">Please wait while we finish authentication.</p>
      </section>
      <DeveloperCredit />
    </main>
  );
}
