# Top 25 React Interview Questions Cheat Sheet

Date: 2026-03-26
Project: PolicyMind Document Service

## How to Use This

These are the 25 React questions most likely to come up first in an interview, answered using real code from this frontend — actual hook usage, actual component names, and the reasoning behind non-obvious choices (like storing request counters in `useRef` instead of `useState`), not generic definitions.

### 1. What is React?
A JavaScript library for building UIs out of composable, declarative components. It powers everything under `frontend/src/` — every screen from login through document upload and Q&A is a tree of React components.

### 2. What is a functional component?
A component written as a plain function returning JSX, using hooks for state/lifecycle instead of a class's `this.state`. Every component in this frontend is functional — `LoginPage`, `UploadPage`, `FloatingMusicPlayer` — with no class components anywhere.

### 3. What is JSX?
HTML-like syntax embedded in JavaScript that the build tooling (`@vitejs/plugin-react`) compiles to `React.createElement(...)` calls. `App.jsx`, `LoginPage.jsx`, and every other component file use it to describe their rendered output.

### 4. What is `createRoot`?
The React 18 API for mounting the app into a DOM node, replacing the older `ReactDOM.render`. Used once, in `frontend/src/main.jsx`: `ReactDOM.createRoot(document.getElementById("root")).render(...)`.

### 5. What is `StrictMode`?
A development-only wrapper that double-invokes certain lifecycle steps and warns about unsafe patterns (like a missing effect cleanup), with zero effect on production builds. `main.jsx` wraps the whole `<App />` tree in it — genuinely useful here given how many components use `useEffect` for real side effects like audio playback and timers.

### 6. What are props?
Read-only inputs passed from a parent component to a child. `ProtectedRoute({ children })` in `App.jsx` is the simplest example — it receives whatever route element it's guarding as a prop and decides whether to render it.

### 7. What is `children`?
The special prop representing JSX nested between a component's opening/closing tags — `<ProtectedRoute><UploadPage /></ProtectedRoute>` passes `<UploadPage />` in as `children`.

### 8. What is state?
Data owned by a component that changes over time and triggers a re-render when it does. `UploadPage` alone tracks state for the selected file, document ID, question, answer data, processing status, and several independent loading flags.

### 9. What is `useState`?
The hook giving a component local, re-render-triggering state plus a setter. Used across login, registration, upload, reset-password, and component-level UI behavior — `LoginPage` alone tracks `username`, `password`, `isLoading`, `successMessage`, and `error` as separate calls.

### 10. What is `useEffect`?
The hook for side effects that run after render — async actions, timers, navigation-driven behavior, and browser API interactions.

### 11. Where is `useEffect` used here?
In `LoginPage` (parsing OAuth error/success state from the URL on mount, preloading reCAPTCHA), `RegisterPage`, `UploadPage` (polling document status), `AuthCallbackPage` (a single effect reading the OAuth callback's query params), `FloatingMusicPlayer` (wiring audio event listeners), and `HeaderTypeTicker` (fetching header content and running a typewriter timer).

### 12. What is `useRef`?
A hook returning a mutable object whose `.current` persists across renders without triggering one when it changes. `UploadPage` uses it two ways: DOM refs (`fileInputRef`, `questionInputRef`) for focus/scroll, and request-ID refs (`uploadRequestIdRef`, `statusRequestIdRef`, `askRequestIdRef`) as stale-response guards for its async calls.

### 13. What is `useMemo`?
A hook that recomputes a derived value only when its dependencies change. `FloatingMusicPlayer` uses `useMemo(() => PLAYLIST[trackIndex], [trackIndex])` for `currentTrack`; `HeaderTypeTicker` uses it for `activeText`.

### 14. What is `useCallback`?
A hook that memoizes a function's identity across renders, so it doesn't needlessly invalidate effects/children that depend on it. `FloatingMusicPlayer` uses it for `playTrack`/`pauseTrack`/`togglePlayPause`, since those are referenced inside `useEffect`s wiring up audio event listeners.

### 15. What is conditional rendering?
Rendering different UI based on state/props. `App.jsx` computes `hideMusicPlayer` from the current route and conditionally renders `<FloatingMusicPlayer />`; `ProtectedRoute` renders either its `children` or a redirect depending on auth state.

### 16. What is routing in React?
Client-side navigation that swaps rendered components based on the URL without a full page reload. This app uses `react-router-dom` throughout.

### 17. What are `BrowserRouter`, `Routes`, and `Route`?
`BrowserRouter` (wrapping `<App />` in `main.jsx`) enables history-based routing; `Routes`/`Route` (in `App.jsx`) define the actual path-to-component mapping — `/`, `/register`, `/reset-password`, `/auth/callback`, `/upload`, `/policy-studio`, `/error`, and a catch-all.

### 18. What is `Navigate`?
A component that performs a redirect declaratively as part of the render tree. Used by `ProtectedRoute` (redirecting to `/` when unauthenticated) and the catch-all route (redirecting any unmatched path to `/error`).

### 19. What is a protected route?
A route that only renders once an auth check passes. `ProtectedRoute` checks `localStorage.getItem("authToken")` — present, it renders `children`; absent, it redirects to `/` — gating `/upload` and `/policy-studio`.

### 20. What is a controlled form?
A form whose input values live in React state rather than the DOM being the source of truth. Login, register, and reset-password pages all bind each input's `value` to `useState` and update it via `onChange`, which makes validation and conditional submit-disabling straightforward.

### 21. How does this frontend call the backend?
Through Axios, via a single shared client in `frontend/src/api/client.js`, imported everywhere an API call is needed rather than each page configuring its own instance.

### 22. Why use a shared API client?
It centralizes the base URL (from `import.meta.env.VITE_API_BASE_URL`), auth token handling, cache-control headers, and a cache-busting timestamp for `GET` requests — one place, applied consistently to every request, instead of each page having to replicate that logic.

### 23. What is an interceptor?
A function that runs on every outgoing request (or incoming response) before it's handled, letting you modify it globally. This frontend's request interceptor attaches a normalized JWT `Authorization` header (skipped for `/auth/*` routes), and unconditionally sets no-cache headers.

### 24. How is auth state handled in the frontend?
The token is stored under `authToken` in `localStorage` after login/registration/OAuth callback, normalized (stripping any duplicate `"Bearer "` prefix) before being attached to requests by the Axios interceptor, and checked directly by `ProtectedRoute` to gate access to authenticated pages.

### 25. How would you summarize your React work from this project?
"I built a React frontend using functional components and hooks — `useState`/`useEffect` for the common cases, `useRef` deliberately for stale-response guarding during async polling (not just DOM access), and `useMemo`/`useCallback` used sparingly where they actually mattered. Routing used `react-router-dom` with a custom `ProtectedRoute` guard, forms were controlled components with async submit handling, and every API call flowed through one shared Axios client with a request interceptor handling auth headers and cache control — all connected to a separate Spring Boot backend."

## Best Files to Review Fast

- `frontend/src/main.jsx`
- `frontend/src/App.jsx`
- `frontend/src/api/client.js`
- `frontend/src/pages/LoginPage.jsx`
- `frontend/src/pages/UploadPage.jsx`
- `frontend/src/components/FloatingMusicPlayer.jsx`
