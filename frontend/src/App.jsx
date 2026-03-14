import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ResetPasswordPage from "./pages/ResetPasswordPage";
import UploadPage from "./pages/UploadPage";
import ErrorPage from "./pages/ErrorPage";
import AboutArchitecturePage from "./pages/AboutArchitecturePage";
import AuthCallbackPage from "./pages/AuthCallbackPage";
import FloatingMusicPlayer from "./components/FloatingMusicPlayer";
import { useLocation } from "react-router-dom";

function ProtectedRoute({ children }) {
  const token = localStorage.getItem("authToken");
  return token ? children : <Navigate to="/" replace />;
}

export default function App() {
  const location = useLocation();
  const hideMusicPlayer = ["/", "/register", "/reset-password", "/auth/callback"].includes(location.pathname);

  return (
    <>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route path="/about" element={<AboutArchitecturePage />} />
        <Route
          path="/upload"
          element={
            <ProtectedRoute>
              <UploadPage />
            </ProtectedRoute>
          }
        />
        <Route path="/error" element={<ErrorPage />} />
        <Route path="*" element={<Navigate to="/error" replace />} />
      </Routes>
      {!hideMusicPlayer && <FloatingMusicPlayer />}
    </>
  );
}
