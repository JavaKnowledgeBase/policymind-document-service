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
    const provider = params.get("provider");
    const reason = params.get("reason");

    if (token) {
      localStorage.setItem("authToken", token);
      navigate("/upload", { replace: true });
      return;
    }

    if (error) {
      const nextParams = new URLSearchParams();
      nextParams.set("oauthError", "1");
      if (provider) {
        nextParams.set("oauthProvider", provider);
      }
      if (reason) {
        nextParams.set("oauthReason", reason);
      }
      navigate(`/?${nextParams.toString()}`, { replace: true });
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
