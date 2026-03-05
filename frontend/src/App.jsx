import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import UploadPage from "./pages/UploadPage";
import ErrorPage from "./pages/ErrorPage";
import AboutArchitecturePage from "./pages/AboutArchitecturePage";
import AuthCallbackPage from "./pages/AuthCallbackPage";
import FloatingMusicPlayer from "./components/FloatingMusicPlayer";

function ProtectedRoute({ children }) {
  const token = localStorage.getItem("authToken");
  return token ? children : <Navigate to="/" replace />;
}

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<LoginPage />} />
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
      <FloatingMusicPlayer />
    </>
  );
}
