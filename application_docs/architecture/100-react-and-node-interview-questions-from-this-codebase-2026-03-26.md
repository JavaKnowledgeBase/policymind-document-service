# 100 React and Node Interview Questions Explained Through This Codebase

Date: 2026-03-26
Project: PolicyMind Document Service
Audience: Interview preparation using real project examples

## Important Context Before You Use This Guide

This repository contains:
- a React frontend in `frontend/`
- a Spring Boot backend in `src/main/java/...`
- Node.js used for frontend tooling, package management, local development, and production build generation

This repository does not contain:
- a Node.js backend server such as Express, NestJS, Fastify, or Koa

So when you explain Node in interviews from this repo, the accurate statement is:

"Node is used here as the JavaScript runtime for frontend tooling and build execution through npm and Vite, not as the backend application server."

Getting this distinction right, unprompted, is itself a signal to an interviewer — it shows you understand what Node *is* (a runtime) rather than conflating it with "the backend," which is a common junior mistake.

## How to Use This Document

Each question is answered in terms of how the concept appears in this codebase — with real component names, real hook usage, and real code excerpts — so you can explain React and Node using actual implementation examples instead of generic theory. Where a design choice has a reason behind it (why request IDs are stored in `useRef` instead of `useState`, why the Axios interceptor skips `/auth/*` routes), the answer explains the reasoning, since that's what a senior-level discussion actually probes for.

Primary files referenced:
- `frontend/package.json`
- `frontend/src/main.jsx`
- `frontend/src/App.jsx`
- `frontend/src/api/client.js`
- `frontend/src/pages/*.jsx`
- `frontend/src/components/*.jsx`

## Section 1: React Basics

### 1. What is React?
React is a JavaScript library for building user interfaces out of composable, declarative components. In this project it powers the entire frontend inside `frontend/src/` — every screen the user sees, from login through document upload and Q&A, is a tree of React components rendered into a single DOM root.

### 2. Why is React used in this project?
It structures the UI into independently reasoned-about pieces instead of one monolithic script manipulating the DOM directly — pages (`LoginPage`, `RegisterPage`, `UploadPage`), route guards (`ProtectedRoute`), and shared cross-cutting UI (`FloatingMusicPlayer`, `HeaderTypeTicker`) are all separate components with their own state and lifecycle, composed together in `App.jsx`.

### 3. What is a React component?
A component is a self-contained, reusable unit of UI and logic — a function that takes props and returns JSX describing what should render. Concrete examples in this project: `LoginPage` (owns its own form state and auth submission logic), `UploadPage` (the most complex screen, owning file upload, polling, and Q&A state), and `FloatingMusicPlayer` (a persistent, cross-route audio widget).

### 4. What is a functional component?
A functional component is a plain JavaScript function that returns JSX, using hooks (`useState`, `useEffect`, etc.) for state and lifecycle instead of a class's `this.state`/`componentDidMount`. Every component in this frontend — there are no class components anywhere in `frontend/src/` — is written this way.

### 5. Why are functional components preferred here?
They compose naturally with hooks, avoid the `this`-binding boilerplate class components require, and keep related logic (a piece of state and the effect that reacts to it) colocated instead of split across separate lifecycle methods. `UploadPage`'s dozen-plus `useState`/`useRef` declarations sitting right next to the `useEffect`s that use them is a direct illustration of that colocation benefit.

### 6. What is JSX?
JSX is syntax that lets you describe a UI tree using HTML-like markup embedded directly in JavaScript, which the build toolchain (Vite's `@vitejs/plugin-react`) compiles down to `React.createElement(...)` calls. Every `.jsx` file in this project — `App.jsx`, `LoginPage.jsx`, `UploadPage.jsx` — uses this syntax to define its rendered structure.

### 7. What is the React root?
The root is the single DOM node React takes ownership of and mounts the entire component tree into. In `frontend/src/main.jsx`:
```jsx
ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);
```
Everything the user sees is a descendant of that one `#root` element in `frontend/index.html`.

### 8. What is `ReactDOM.createRoot`?
It's the React 18 API for creating the root rendering container, replacing the older `ReactDOM.render`. It enables React 18's concurrent rendering features (though this app doesn't lean on concurrent features explicitly — it uses the modern API as the correct baseline for a React 18 project). This project calls it exactly once, in `main.jsx`, against `document.getElementById("root")`.

### 9. What is `React.StrictMode`?
It's a development-only wrapper that renders certain lifecycle steps twice, warns about legacy/unsafe patterns, and helps surface impure render logic or missing effect cleanup — none of which produces any output or overhead in production builds. `main.jsx` wraps the entire `<App />` tree in `<React.StrictMode>`.

### 10. Why use `StrictMode`?
It catches subtle bugs early in development — an effect without a cleanup function, or a component that isn't safe to render twice — before they turn into hard-to-reproduce production issues. Given how many components in this app use `useEffect` for real side effects (audio playback, timers, network fetches — see `FloatingMusicPlayer` and `HeaderTypeTicker`), `StrictMode`'s double-invoke-in-dev behavior is a genuinely useful safety net for catching a missing cleanup function.

## Section 2: Components, Props, and Composition

### 11. What are props?
Props are read-only inputs a parent passes down to a child component. `ProtectedRoute({ children })` in `App.jsx` is the clearest example — it receives whatever route element it's wrapping as its `children` prop and decides whether to render it or redirect, without needing to know anything about what that child actually is.

### 12. What is `children` in React?
`children` is the special prop representing whatever JSX was nested between a component's opening and closing tags. `ProtectedRoute` is used like `<ProtectedRoute><UploadPage /></ProtectedRoute>` in the route table, and internally reads `children` to decide what to render once the auth check passes.

### 13. What is component composition?
Composition means assembling larger UI out of smaller, independent components rather than one large component doing everything. `App.jsx` composes the route table (each page a separate component) together with always-present cross-cutting UI like `<FloatingMusicPlayer />`, rendered conditionally alongside whichever route is active.

### 14. Why is composition useful in this codebase?
It keeps `App.jsx` itself small and focused purely on routing/layout concerns, while all the page-specific complexity (form state, upload logic, polling) stays encapsulated inside each page component — `UploadPage`'s dozen or so pieces of state never leak into `App.jsx` or any other page.

### 15. What is conditional rendering?
Conditional rendering shows different UI based on state or props. `App.jsx` computes whether to hide the floating music player based on the current route:
```jsx
const hideMusicPlayer = ["/", "/register", "/reset-password", "/auth/callback"].includes(location.pathname);
...
{!hideMusicPlayer && <FloatingMusicPlayer />}
```

### 16. How is conditional rendering used here?
Beyond hiding the music player on auth-related routes, it drives route protection (`ProtectedRoute` rendering either `children` or a `<Navigate>` redirect), loading/error/success states across the auth pages (`LoginPage`'s `successMessage`/`error` state rendered conditionally), and `UploadPage`'s multi-step workflow, which reveals its "ask a question" step only once a document has finished processing.

### 17. What is a controlled component?
A controlled component keeps its input's current value in React state, with the state as the single source of truth rather than the DOM. `LoginPage`, `RegisterPage`, and the reset-password flow all bind each `<input>`'s `value` to a piece of `useState` and update it via `onChange`, rather than reading the DOM value imperatively on submit.

### 18. Why use controlled forms?
They make validation, conditional submit-button disabling, and programmatic value manipulation straightforward, since the current value is always available in state rather than requiring a DOM read. It also makes it trivial to reset a form (`setForm(initialState)`) or derive submit-readiness (`disabled={!username || !password}`) directly from state that's already there.

### 19. What is a reusable component?
A reusable component is written once and rendered from multiple places without duplicating its logic. `FloatingMusicPlayer` and `HeaderTypeTicker` are the clearest examples — both are shared UI rendered from `App.jsx` (or a shared layout) rather than being duplicated inside each page that wants a music widget or a typewriter header.

### 20. Why are page and component folders separated?
`frontend/src/pages/` holds route-level screens (one per URL, each owning that screen's specific state and submit logic), while `frontend/src/components/` holds smaller, reusable pieces used across multiple pages or persistently across the whole app. That separation makes it immediately obvious, from folder structure alone, what's a full screen versus what's a shared building block.

## Section 3: State and Hooks

### 21. What is state in React?
State is data owned by a component that can change over time and triggers a re-render when it does. `useState` is used extensively across this frontend — `UploadPage` alone tracks state for the selected file, the resulting document ID, the current question, the answer/review data, the document's processing status, several loading flags, and status/error messages.

### 22. What is `useState`?
`useState` is the hook that gives a functional component a piece of local, re-render-triggering state and a setter function. `LoginPage`, `RegisterPage`, `UploadPage`, and `FloatingMusicPlayer` all use it — for example, `LoginPage` tracks `username`, `password`, `isLoading`, `successMessage`, and `error` as separate `useState` calls.

### 23. Why does `UploadPage` use many `useState` calls?
Because it's genuinely tracking many independent pieces of UI state at once: `file`, `documentId`, `question`, `answerData`, `reviewData`, `documentStatus`, `isUploading`, `isAsking`, `isReviewing`, `message`, `error`, and `activeStep`. Splitting these into separate `useState` calls (rather than one large state object) means each update only concerns the specific piece of UI it affects, and keeps the component's re-render triggers granular and easy to reason about.

### 24. What is `useEffect`?
`useEffect` runs a side effect after rendering, optionally re-running when its dependency array changes, and optionally returning a cleanup function. This codebase uses it for reading query-string state on mount (`LoginPage`, `AuthCallbackPage`), polling a document's processing status (`UploadPage`), and wiring up audio event listeners and timers (`FloatingMusicPlayer`, `HeaderTypeTicker`).

### 25. Where is `useEffect` used here?
It appears in `LoginPage` (parsing `oauthError`/`successMessage` from the URL, and preloading reCAPTCHA on mount), `RegisterPage`, `UploadPage` (status polling), `AuthCallbackPage` (a single effect that reads the OAuth callback's query params and either stores the token and navigates to `/upload`, or navigates back to `/` with an error), `FloatingMusicPlayer` (wiring `ended`/`play`/`pause` audio listeners and autoplay-on-track-change), and `HeaderTypeTicker` (fetching header content, plus a typewriter-effect timer).

### 26. Why is `useEffect` important in frontend apps?
Because real UI code constantly needs to synchronize with things outside React's own render cycle — network requests, timers, browser storage, the DOM, media elements — and `useEffect` is React's designated place for that synchronization to happen safely, with a defined point (the returned cleanup function) to tear it back down when the component unmounts or its dependencies change.

### 27. What is `useRef`?
`useRef` returns a mutable object whose `.current` property persists across renders without causing a re-render when it changes. `UploadPage` uses several: `workspaceTopRef`, `stepTwoRef`, and `questionInputRef`/`fileInputRef` for scrolling/focus management, and — more interestingly — `uploadRequestIdRef`, `statusRequestIdRef`, and `askRequestIdRef` as **stale-response guards**.

### 28. Why use refs in `UploadPage`?
Two distinct reasons. First, DOM refs (`fileInputRef`, `questionInputRef`, `stepTwoRef`) drive imperative actions like focusing an input or scrolling a section into view — things React's declarative model doesn't handle directly. Second, and more subtly, the request-ID refs (`uploadRequestIdRef`, `statusRequestIdRef`, `askRequestIdRef`) are incremented on each new async call and checked when that call resolves; if the ref's value has since moved on (because the user triggered a newer request), the stale response is discarded. This pattern deliberately uses a ref instead of state, because incrementing a request counter is bookkeeping that should never itself trigger a re-render.

### 29. What is `useMemo`?
`useMemo` recomputes and caches a derived value only when its dependencies change, avoiding redundant recalculation on every render. `FloatingMusicPlayer` uses `useMemo(() => PLAYLIST[trackIndex], [trackIndex])` to derive `currentTrack`, and `HeaderTypeTicker` uses `useMemo(() => items[activeIndex] || "", [items, activeIndex])` for `activeText`.

### 30. Why use `useMemo` sparingly?
Because memoization itself has a cost (storing the previous dependencies and comparing them every render), and for cheap computations like a plain array index lookup, that bookkeeping can cost more than just recalculating the value directly. This codebase uses `useMemo` in exactly the two places above — small, clearly-scoped derivations — rather than wrapping every computed value in it reflexively.

### 31. What is `useCallback`?
`useCallback` memoizes a function reference itself, so it doesn't change identity on every render unless its dependencies do — useful when that function reference is a dependency of another hook (like `useEffect`) or passed to a memoized child. `FloatingMusicPlayer` uses it for `playTrack`, `pauseTrack`, and `togglePlayPause`.

### 32. Why is `useCallback` useful in UI components?
Because `FloatingMusicPlayer`'s audio-control functions are referenced inside `useEffect`s that wire up event listeners — without `useCallback`, a new function identity on every render would cause those effects to think their dependencies changed and re-run (re-attaching listeners) far more often than necessary.

### 33. What is derived state?
Derived state is a value computed from other state rather than stored redundantly in its own `useState`. `currentTrack` (derived from `trackIndex` via `useMemo`) and `activeText` (derived from `items`/`activeIndex`) are both examples — neither is its own independent piece of state that could drift out of sync with its source.

### 34. Why avoid unnecessary duplicate state?
Because two pieces of state that represent overlapping information can get out of sync — if `currentTrack` were its own `useState` instead of being derived from `trackIndex`, changing `trackIndex` and forgetting to also update `currentTrack` would silently desync the UI from the actual playlist position. This frontend mostly computes such values instead of storing redundant copies, which eliminates that whole category of bug by construction.

### 35. What triggers a React re-render?
A component re-renders when its own state changes, when props it receives change, or when its parent re-renders (unless memoized). This is exactly why `UploadPage` keeps the frequently-changing request-ID bookkeeping in `useRef` (which never triggers a re-render on mutation) rather than `useState` — that data needs to persist across renders but has no reason to *cause* one every time it's incremented.

## Section 4: Routing and Navigation

### 36. What is client-side routing?
Client-side routing swaps out which components render based on the URL, without a full page reload/server round-trip for each navigation. This project uses `react-router-dom` for exactly this — clicking between login, upload, and other screens never triggers a browser page reload.

### 37. Where is routing configured here?
`frontend/src/main.jsx` wraps the whole app in `<BrowserRouter>`, and `frontend/src/App.jsx` defines the actual route table with `<Routes>`/`<Route>`.

### 38. What is `BrowserRouter`?
It's the React Router component that enables HTML5 history-based routing (real, clean URLs backed by the browser's History API, rather than hash-based routing). This app wraps `<App />` with it once, in `main.jsx`.

### 39. What are `Routes` and `Route`?
`Routes` is the container that matches the current URL against its child `Route` elements and renders the first match; each `Route` maps a `path` to an `element`. `App.jsx` defines routes for `/`, `/register`, `/reset-password`, `/auth/callback`, `/about`, `/upload`, `/policy-studio`, `/error`, and a catch-all `path="*"`.

### 40. What is `Navigate`?
`Navigate` is a component that performs a redirect declaratively, as part of the render tree, rather than imperatively in an event handler. This app uses it in two places: `ProtectedRoute` redirects to `/` when there's no auth token, and the catch-all route (`<Route path="*" element={<Navigate to="/error" replace />} />`) redirects any unmatched URL to an error page.

### 41. What is a protected route?
A protected route only renders its content once an authentication check passes. `ProtectedRoute` in `App.jsx` checks `localStorage.getItem("authToken")` — if it's present, it renders `children`; if not, it renders `<Navigate to="/" replace />`, sending the user back to the login page.

### 42. Why is route protection useful here?
Because `/upload` and `/policy-studio` are meant to be used only by an authenticated user — without `ProtectedRoute` wrapping them, anyone with the URL could load the upload workflow's UI even with no valid session, and any API calls it made would simply fail server-side with a 401 instead of being blocked at the UI layer first.

### 43. What is `useNavigate`?
`useNavigate` returns a function for programmatic navigation, used inside event handlers or effects rather than declaratively in JSX. Login, register, reset-password, `AuthCallbackPage`, and `UploadPage` all use it — for instance, `LoginPage` calls it after a successful login to send the user to `/upload`.

### 44. What is `useLocation`?
`useLocation` returns the current route's location object (pathname, search string, and any state passed via `Navigate`/`useNavigate`). `App.jsx` uses it to compute `hideMusicPlayer` from `location.pathname`, and the auth pages use it to read query params or navigation `state` (like a `successMessage` passed from one page to another after a redirect).

### 45. Why is `useLocation` useful in this app?
It supports route-aware behavior that depends on more than just which component is rendering — hiding the music player specifically on auth-flow routes, or `AuthCallbackPage` reading `token`/`error`/`provider`/`reason` straight out of the OAuth redirect's query string to decide whether the login succeeded or failed.

## Section 5: Forms and User Input

### 46. How are forms handled in this frontend?
Through component state (controlled inputs), event handlers for changes and submission, `async`/`await` calls to the API client, and conditional rendering of loading/success/error messages based on that state — no external form library is used; the pattern is hand-rolled and consistent across `LoginPage`, `RegisterPage`, and the reset-password flow.

### 47. Why are login and registration pages good React examples?
Because they exercise nearly every core frontend concern at once: controlled inputs, `onSubmit` handling, an async API call with a loading state, success/error branching, conditional messages, and a post-success navigation — `RegisterPage`, for instance, posts to `/auth/register` and navigates to `/` passing a `successMessage` through router state so the login page can display a "registration successful" banner.

### 48. What is form submission handling in React?
It's capturing the `onSubmit` event (typically calling `e.preventDefault()` to stop the browser's native form submission/page reload), reading the already-controlled input state, and sending it to an API — visible in both the auth pages and, more elaborately, in `UploadPage`'s file-submission handler.

### 49. What is `FormData` and where is it used?
`FormData` builds a `multipart/form-data` payload, which is required for file uploads (plain JSON can't carry binary file content). `UploadPage.handleSubmit` builds one directly:
```js
const formData = new FormData();
formData.append("file", file);
...
await client.post("/upload", formData, {
  headers: {
    "Content-Type": "multipart/form-data",
    "X-Upload-Request-Id": `upload-${Date.now()}-${requestId}`,
  },
});
```

### 50. Why use `FormData` here?
Because the backend's `/upload` endpoint expects a `MultipartFile` (see the Java-side `DocumentService.submitDocument(MultipartFile file)`), and multipart is the standard way to send binary file content over HTTP alongside other form fields — plain `application/json` has no native way to embed raw file bytes.

### 51. How are errors shown to users in this app?
Each page keeps an `error` (or similarly-named) piece of state, set inside a `catch` block after a failed API call, and renders it conditionally near the relevant form or workflow step — `UploadPage` and the auth pages both follow this pattern consistently rather than using a global toast/notification system.

### 52. Why track loading state in forms?
To disable the submit control and show feedback while an async request is in flight, preventing duplicate submissions (a user double-clicking "Upload" while the first request is still processing) and giving the user a clear signal that something is happening. This codebase uses explicitly-named flags — `isLoading`, `isRegistering`, `isUploading`, `isAsking` — rather than one generic `loading` boolean, since `UploadPage` in particular has several independent async operations that can be in flight (or not) at different times.

### 53. Why clear or normalize inputs after auth/callback actions?
To avoid carrying a stale, malformed, or partially-set token forward into subsequent requests. `AuthCallbackPage` normalizes the token it receives from the OAuth redirect before storing it, and the Axios client itself (see Q64) re-normalizes whatever's in `localStorage` on every outgoing request, as a second line of defense.

### 54. How is token storage handled in the frontend?
The auth token is stored under the `authToken` key in `localStorage` after a successful login, OAuth callback, or registration flow, and read back by `ProtectedRoute` (to gate protected pages) and by the Axios request interceptor in `client.js` (to attach it to outgoing requests).

### 55. What is the tradeoff of using `localStorage`?
`localStorage` is simple to use and persists across page refreshes and browser restarts (unlike in-memory state, which would force a re-login on every reload), but it's readable by any JavaScript running on the page — meaning an XSS vulnerability anywhere in the app could exfiltrate the token, which is a real tradeoff against alternatives like an httpOnly cookie that JavaScript can't read at all. Knowing this tradeoff, and being able to name the XSS risk explicitly, is a stronger interview answer than just describing what `localStorage` does.

## Section 6: API Calls and Frontend Architecture

### 56. How does the frontend call the backend?
Through a single shared Axios instance created in `frontend/src/api/client.js`, imported by every page/component that needs to make an API call, rather than each screen configuring its own `axios.create(...)` or raw `fetch`.

### 57. Why create a shared Axios client?
It centralizes cross-cutting request concerns — the base URL, auth header attachment, cache-control headers, and a cache-busting timestamp for `GET` requests — in exactly one place, so every call automatically gets consistent behavior instead of each page having to remember to replicate it.

### 58. What is an Axios interceptor?
An interceptor is a function that runs on every outgoing request (or incoming response) before it's actually sent/handled, letting you modify it globally. `client.js`'s request interceptor does several things at once: it skips attaching an `Authorization` header for any `/auth/*` request, otherwise reads and normalizes the stored token and sets `Authorization: Bearer <token>`; it always sets `Cache-Control`/`Pragma`/`Expires` headers to disable caching; and for `GET` requests it appends a `_ts: Date.now()` query parameter.

### 59. Why skip the auth header for `/auth/` requests?
Because login, registration, and password-reset requests happen precisely when the user may not have a valid token yet (or is trying to replace an expired/invalid one) — automatically attaching a stale or nonexistent token to those calls would be pointless at best, and could cause the backend to reject the request based on a bad token that had nothing to do with the actual login attempt.

### 60. What is `import.meta.env`?
It's Vite's mechanism for exposing build-time environment variables to frontend code (the browser equivalent of Node's `process.env`, but resolved at build time by Vite rather than read at runtime). `client.js` reads `import.meta.env.VITE_API_BASE_URL`, falling back to `"/api"` if it isn't set.

### 61. Why is environment-based API configuration useful?
It lets the exact same frontend build point at different backend URLs depending on environment — local development, Docker Compose, or a deployed AWS/EC2 target — purely through the `VITE_API_BASE_URL` environment variable at build time, with no source code changes between environments. This project's `.env.example` and `.env.production` files exist precisely to support that.

### 62. Why add cache-control headers in the API client?
Because this app needs fresh data on nearly every call — document processing status in particular must never be served from a stale browser or intermediary cache, or the UI would show an outdated status indefinitely. The interceptor unconditionally sets `Cache-Control: no-store, no-cache, max-age=0, must-revalidate` plus `Pragma: no-cache` and `Expires: 0` on every request to guarantee that.

### 63. Why add a timestamp parameter to GET requests?
Some caching layers (particularly aggressive browser or proxy caches) can still cache a `GET` request by URL even with strong cache-control headers present, especially on flaky intermediary configurations. Appending `_ts: Date.now()` makes each `GET` request's URL unique, which is a defensive belt-and-suspenders technique on top of the headers — directly relevant for this app's status-polling behavior in `UploadPage`, where the same status endpoint is hit repeatedly and must never return a cached, stale answer.

### 64. Why normalize bearer tokens on the client?
Because a token can end up prefixed with `"Bearer "` more than once if it passes through more than one code path that adds the prefix (for instance, if a value already containing `"Bearer <token>"` gets stored and then the interceptor blindly prepends `"Bearer "` again) — the interceptor's `normalizeStoredToken` strips any existing `"Bearer "` prefix via regex before re-adding exactly one, preventing a malformed `Authorization: Bearer Bearer <token>` header that the backend would reject.

### 65. What is separation of concerns in the frontend here?
Routing and top-level layout live in `App.jsx`; app bootstrapping (mounting React, wrapping in `StrictMode`/`BrowserRouter`) lives in `main.jsx`; all network/API concerns live in `api/client.js`; and page-specific business logic lives in each page component. No single file mixes routing configuration with network configuration with UI logic — each has exactly one job.

## Section 7: React Rendering, Performance, and UX

### 66. What is declarative UI?
In React, you describe *what* the UI should look like for the current state, and React figures out *how* to update the actual DOM to match — you don't manually create, update, or remove DOM nodes yourself. Every component in this project follows that model; there is no direct `document.querySelector`/manual DOM mutation anywhere in the page components.

### 67. Why is declarative UI better than manual DOM manipulation?
Because a screen like `UploadPage` — with a dozen-plus pieces of state and several conditionally-rendered steps — would be extremely error-prone to keep in sync with the DOM manually (imagine hand-writing the show/hide logic for every step, loading spinner, and error message as raw DOM calls). Declaring "here's what should render given this state" and letting React reconcile it is what makes a screen this stateful tractable at all.

### 68. How does the upload flow show practical React state modeling?
`UploadPage` breaks the user's journey into discrete steps (tracked via `activeStep`) and reflects processing progress purely through state variables (`documentStatus`, `isUploading`, `isAsking`, etc.) rather than imperatively toggling DOM visibility — the UI at any moment is a pure function of that state, which is what makes the multi-step, asynchronous workflow (upload → poll for status → ask a question → show an answer) manageable.

### 69. Why use refs instead of state for request counters?
Because `uploadRequestIdRef`/`statusRequestIdRef`/`askRequestIdRef` exist purely as internal bookkeeping to detect and discard stale async responses (see Q27/28) — incrementing them should never itself cause a re-render, since the counter's value isn't something the UI displays. Using `useState` for this would cause an unnecessary re-render on every single async call, purely to update a number nothing on screen shows.

### 70. Why is route-level code organization helpful?
Because it keeps large, stateful screens like `UploadPage`, `LoginPage`, and `RegisterPage` focused on exactly one job each — one screen's worth of state and logic doesn't bleed into another's, and a developer opening `UploadPage.jsx` can be confident they're seeing everything relevant to the upload workflow in one place.

### 71. What is a side effect related to browser APIs?
Any interaction with something outside React's own rendering — reading/writing `localStorage`, controlling `<audio>` playback, managing focus/scroll, or setting timers. This app performs all of these: `client.js` reads/writes `localStorage` for the auth token, `FloatingMusicPlayer` directly controls an `<audio>` element's playback, and `UploadPage` manages focus/scroll via refs.

### 72. Why are timers and effects tricky in React?
Because a timer or interval started inside `useEffect` keeps running even after the component that started it unmounts, unless the effect's cleanup function explicitly clears it — a classic source of "setState on an unmounted component" warnings and memory leaks. `HeaderTypeTicker`'s typewriter effect (built on `setInterval`/`setTimeout`, keyed off constants `TOTAL_WINDOW_MS = 20000` and `TYPING_WINDOW_MS = 14000`) and `FloatingMusicPlayer`'s hide-playlist timeout both need — and have — careful cleanup in their effects' return functions.

### 73. What does React mean by one-way data flow?
Data flows down the component tree via props, and the only way a child influences a parent is by calling a function the parent passed down (typically a state setter, passed as a prop or callback) — state itself is never mutated directly from below. `ProtectedRoute` receiving `children` from its parent route, and every controlled input calling its owning component's `onChange` handler rather than mutating state directly, both follow this pattern.

### 74. Why are fallback routes important?
They handle any URL that doesn't match a defined route gracefully instead of showing a blank page or a router error. `App.jsx`'s catch-all `<Route path="*" element={<Navigate to="/error" replace />} />` redirects any unmatched path to a dedicated `/error` page, giving the user a clear, on-brand "this isn't a valid page" experience.

### 75. Why is UX state management important in interview discussions?
Because real frontend work is rarely just rendering static markup — it's coordinating async operations (uploads, polling, LLM calls that can take seconds), surfacing errors without losing the user's place, and keeping multiple in-flight requests from stepping on each other (which is exactly the problem `UploadPage`'s request-ID refs solve). Being able to walk through that concrete example is a much stronger answer than describing UX state management abstractly.

## Section 8: Node Basics in the Context of This Repo

### 76. What is Node.js?
Node.js is a JavaScript runtime built on Chrome's V8 engine, letting JavaScript run outside a browser. In this repo it exclusively runs frontend tooling — npm's package management and Vite's dev server/bundler — never as an application server handling HTTP requests for end users.

### 77. Is Node used as the backend server in this project?
No. The backend server is Spring Boot, written in Java (`src/main/java/com/policymind/document/`). Node is used only for frontend development and build tooling — this is worth stating explicitly and confidently in an interview, since assuming "Node backend" from seeing a `frontend/package.json` is a common but incorrect inference.

### 78. Why is Node still important here?
Because the entire React frontend's development and build workflow depends on it — installing dependencies, running the local dev server, and producing the optimized production bundle that actually gets served to users are all Node-powered steps, even though none of them involve Node running as a request-handling server.

### 79. What is npm?
npm is Node's package manager, used here to install frontend dependencies (`react`, `axios`, `react-router-dom`, ...) and to run the project's defined scripts — `npm run dev`, `npm run build`, `npm run preview`.

### 80. Where is npm usage defined?
In `frontend/package.json`, under its `"scripts"` section:
```json
{
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  }
}
```

### 81. What is `package.json`?
It's the manifest file defining a Node package's metadata, scripts, dependencies, and devDependencies. `frontend/package.json` defines this frontend's identity, its three npm scripts, and its full dependency list.

### 82. What are dependencies vs devDependencies?
Dependencies are needed by the running application (bundled into the production output); devDependencies are only needed during development/build and never ship in the final bundle. Here, `dependencies` are `react ^18.3.1`, `react-dom ^18.3.1`, `axios ^1.8.3`, and `react-router-dom ^6.30.1`; `devDependencies` are `vite ^5.4.14` and `@vitejs/plugin-react ^4.4.1` — the build tool and its React plugin, needed only to produce the bundle, not to run inside it.

### 83. What does `type: module` mean in `package.json`?
It tells Node to treat `.js` files in this package as native ES modules (`import`/`export`) by default, rather than the older CommonJS (`require`/`module.exports`) format. That aligns with how Vite and this project's own source code are written — consistently using `import`/`export` syntax throughout.

### 84. What are npm scripts?
They're named shell commands defined under `"scripts"` in `package.json`, invoked with `npm run <name>`. This project defines exactly three: `dev`, `build`, and `preview`.

### 85. What does `npm run dev` do here?
It runs `vite`, starting Vite's development server with hot module replacement — code changes in `frontend/src/` are reflected in the browser near-instantly without a full page reload, which is what makes local development fast.

### 86. What does `npm run build` do here?
It runs `vite build`, producing an optimized, minified, production-ready static bundle (JS, CSS, assets) that's what actually gets deployed and served — this is the artifact the `frontend/Dockerfile`/`Dockerfile.release` copies into the final image alongside the nginx config.

### 87. What does `npm run preview` do here?
It runs `vite preview`, serving the already-built production bundle locally so you can sanity-check the actual production output before deploying it — distinct from `npm run dev`, which serves unbundled source through Vite's dev server rather than the real production artifact.

### 88. What is Vite?
Vite is the build tool and dev server this project uses instead of older alternatives like Create React App/webpack — it serves source modules natively over ESM during development (avoiding a slow full bundle rebuild on every change) and uses Rollup under the hood for optimized production builds.

### 89. Why is Vite a good fit for this repo?
It's fast in both development (near-instant server start and hot updates, since it doesn't bundle everything upfront) and production (Rollup-based tree-shaken output), and it has first-class, low-configuration support for React via `@vitejs/plugin-react` — exactly the profile a small-to-medium React frontend like this one needs, without the configuration overhead of a more manual webpack setup.

### 90. What does `import.meta.env` have to do with Node/Vite?
`import.meta.env` is a browser-side API that Vite implements and populates at build time — Vite itself runs on Node (during `npm run dev`/`npm run build`) to read environment variables and `.env` files from the filesystem, then injects the ones prefixed `VITE_` into the client bundle so browser code can read them via `import.meta.env.VITE_API_BASE_URL`.

### 91. What is a module in JavaScript/Node terms?
A module is a single file that can `export` values and `import` values from other files, giving each file its own scope rather than everything living in one shared global namespace. This frontend uses ES module syntax throughout — `import axios from "axios"`, `export default function LoginPage() { ... }`.

### 92. How are modules used in this frontend?
Every component, page, and utility file imports exactly what it needs — `App.jsx` imports each page component and `ProtectedRoute`; `client.js` is imported by any page that needs to make an API call — keeping dependencies between files explicit and traceable rather than relying on implicit global state.

### 93. What is the purpose of the `node_modules` directory?
It's where npm physically installs every package this project depends on, both direct (`react`, `axios`) and transitive (their own dependencies). Vite, at both dev-server and build time, resolves imports like `import axios from "axios"` against the contents of `frontend/node_modules/`.

### 94. Why should you not hand-edit `node_modules`?
Because it's entirely generated, disposable output — deleting it and running `npm install` again reconstructs it from `package.json`/`package-lock.json`, silently discarding any manual edits. It's also excluded from version control (see `.gitignore`), so hand-edits wouldn't even be shared with anyone else on the team.

### 95. What is a lockfile and why is it useful?
A lockfile (`frontend/package-lock.json`) pins the exact resolved version of every dependency and transitive dependency, not just the version *ranges* declared in `package.json` (like `^18.3.1`). Without it, two different `npm install` runs on the same `package.json` could resolve slightly different transitive versions over time as new compatible releases are published.

### 96. Why is reproducible dependency installation important?
Because an inconsistency between what a developer has installed locally, what CI installs, and what actually ships in a Docker build is exactly the kind of "works on my machine" problem that costs real debugging time — the lockfile guarantees every one of those environments resolves the identical dependency tree, and CI (see `.github/workflows/ci.yml`) typically uses `npm ci`, which installs strictly from the lockfile and fails if it's out of sync with `package.json`.

### 97. What is the difference between the browser runtime and Node runtime?
The actual React application code (`App.jsx`, every page and component) runs in the end user's browser, using browser APIs like the DOM, `localStorage`, and `<audio>`. Tooling code — npm scripts, Vite itself — runs in Node, using Node APIs like the filesystem, to read source files and produce the bundle the browser will eventually execute. This repo genuinely uses both runtimes, but for entirely different purposes, which is the nuance worth being precise about in an interview.

### 98. Why is it important not to confuse Node tooling with a Node backend?
Because an interviewer asking "tell me about your Node backend experience" based on seeing this repo would be testing exactly this distinction — claiming backend experience you don't have from this project (there is no Express/NestJS/Fastify server here) is easy to catch and would undermine credibility on everything else you say about the project. The honest, precise answer ("Node here is frontend tooling; Spring Boot is the backend") is also the stronger one.

### 99. How should you explain Node experience from this codebase honestly?
Say that you used Node for frontend package management (npm), local development workflow (the Vite dev server via `npm run dev`), and environment-driven, ES-module-based production builds (`npm run build`, `import.meta.env`) — not for implementing backend APIs, since those are all handled by Spring Boot in this project.

### 100. If asked "How do React and Node work together in this project?", what should you say?
React provides the UI component model, client-side routing, hooks-based state/effects, and browser-side interaction logic — everything the end user's browser actually executes. Node powers the frontend's development and build toolchain: npm installs and manages dependencies, and Vite (running on Node) serves the app during development and bundles it for production. The resulting static frontend bundle then communicates with the separate Spring Boot backend purely over HTTP, through the shared Axios client in `api/client.js` — Node itself never handles a single one of those HTTP requests server-side.

## Strong Interview Summary

If you need a short summary, use this:

"In this project, React builds the frontend through functional components and hooks — `useState`/`useEffect` for the common cases, plus deliberate use of `useRef` for stale-response guarding during async polling, and `useMemo`/`useCallback` used sparingly where they actually matter, like `FloatingMusicPlayer`'s audio controls. Routing is handled with `react-router-dom`, including a `ProtectedRoute` component gating authenticated pages. All API calls go through one shared Axios client with a request interceptor that manages auth headers, cache-busting, and bearer-token normalization in a single place. Node's role is strictly frontend tooling — npm for dependency management and Vite for the dev server and production build — while Spring Boot, not Node, serves the actual backend APIs this frontend talks to."

## Best Files to Review Before an Interview

- `frontend/package.json`
- `frontend/src/main.jsx`
- `frontend/src/App.jsx`
- `frontend/src/api/client.js`
- `frontend/src/pages/LoginPage.jsx`
- `frontend/src/pages/RegisterPage.jsx`
- `frontend/src/pages/ResetPasswordPage.jsx`
- `frontend/src/pages/UploadPage.jsx`
- `frontend/src/pages/AuthCallbackPage.jsx`
- `frontend/src/components/FloatingMusicPlayer.jsx`
- `frontend/src/components/HeaderTypeTicker.jsx`
