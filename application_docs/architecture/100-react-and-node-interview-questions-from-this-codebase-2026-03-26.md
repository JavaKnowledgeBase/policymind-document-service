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

## How to Use This Document

Each question is answered in terms of how the concept appears in this codebase so you can explain React and Node using real implementation examples instead of generic theory.

Primary files referenced:
- `frontend/package.json`
- `frontend/src/main.jsx`
- `frontend/src/App.jsx`
- `frontend/src/api/client.js`
- `frontend/src/pages/*.jsx`
- `frontend/src/components/*.jsx`

## Section 1: React Basics

### 1. What is React?
React is a JavaScript library for building user interfaces. In this project it powers the frontend inside `frontend/src/`.

### 2. Why is React used in this project?
It helps structure the UI into reusable components such as pages, auth screens, upload flow screens, and shared UI components like the music player.

### 3. What is a React component?
A component is a reusable unit of UI and logic. Examples in this project include `LoginPage`, `UploadPage`, `FloatingMusicPlayer`, and `AuthShell`.

### 4. What is a functional component?
A functional component is a React component written as a function. Almost every component in this frontend is a functional component.

### 5. Why are functional components preferred here?
They work naturally with hooks and keep the code concise and modern.

### 6. What is JSX?
JSX is syntax that lets you write UI structure inside JavaScript. Files like `App.jsx` and `LoginPage.jsx` use JSX to define the UI tree.

### 7. What is the React root?
The root is where the React app mounts into the DOM. In `frontend/src/main.jsx`, `ReactDOM.createRoot(...)` mounts the app into the `root` element.

### 8. What is `ReactDOM.createRoot`?
It is the React 18 API for creating the root rendering container. This repo uses it in `main.jsx`.

### 9. What is `React.StrictMode`?
It is a development helper that highlights unsafe patterns. This project wraps the app in `React.StrictMode` in `main.jsx`.

### 10. Why use `StrictMode`?
It helps catch side-effect and lifecycle issues during development.

## Section 2: Components, Props, and Composition

### 11. What are props?
Props are inputs passed from parent to child components. `ProtectedRoute({ children })` is a clear example of props usage in `App.jsx`.

### 12. What is `children` in React?
`children` is a special prop representing nested JSX. `ProtectedRoute` uses `children` to wrap protected UI.

### 13. What is component composition?
Composition means building larger UI structures from smaller components. `App.jsx` composes pages and shared components such as `FloatingMusicPlayer`.

### 14. Why is composition useful in this codebase?
It keeps the app modular, readable, and easier to evolve.

### 15. What is conditional rendering?
Conditional rendering means showing different UI based on state or conditions. `App.jsx` hides the music player for auth-related routes.

### 16. How is conditional rendering used here?
Examples include showing route protection, hiding/showing UI sections, rendering error messages, and displaying loading states.

### 17. What is a controlled component?
A controlled component stores form input values in React state. Login, register, and reset password forms in this project are controlled.

### 18. Why use controlled forms?
They make validation, error handling, and submit behavior easier to manage.

### 19. What is a reusable component?
A reusable component can appear in multiple places without duplicating code. Examples include layout-related components and shared UI elements.

### 20. Why are page and component folders separated?
Pages represent route-level screens, while components represent reusable UI pieces. That separation keeps the frontend organized.

## Section 3: State and Hooks

### 21. What is state in React?
State is data that changes over time and triggers re-renders. `useState` is used extensively across the frontend.

### 22. What is `useState`?
`useState` is the hook for local component state. `LoginPage`, `RegisterPage`, `UploadPage`, and the music player all use it.

### 23. Why does `UploadPage` use many `useState` calls?
Because it tracks multiple UI concerns: file, document ID, question, status, answer data, loading states, messages, and errors.

### 24. What is `useEffect`?
`useEffect` runs side effects after rendering. This codebase uses it for auth flow handling, polling-like behavior, DOM updates, and media interaction.

### 25. Where is `useEffect` used here?
It appears in many pages and components including `LoginPage`, `RegisterPage`, `UploadPage`, `AuthCallbackPage`, `FloatingMusicPlayer`, and `HeaderTypeTicker`.

### 26. Why is `useEffect` important in frontend apps?
Because UI code often needs to sync with async operations, route changes, timers, browser APIs, or focus behavior.

### 27. What is `useRef`?
`useRef` stores mutable values or DOM references without triggering re-renders. `UploadPage` uses refs for inputs, scroll targets, and request tracking.

### 28. Why use refs in `UploadPage`?
They help manage DOM focus/scrolling and track in-flight request IDs safely across renders.

### 29. What is `useMemo`?
`useMemo` memoizes computed values. It is used in components like `HeaderTypeTicker` and `FloatingMusicPlayer` for derived values.

### 30. Why use `useMemo` sparingly?
Because memoization has its own cost and should only be used when it provides real value. This repo uses it in limited, meaningful places.

### 31. What is `useCallback`?
`useCallback` memoizes function references. `FloatingMusicPlayer` uses it for event-driven audio control helpers.

### 32. Why is `useCallback` useful in UI components?
It helps stabilize function references when a component has effects or handlers that depend on them.

### 33. What is derived state?
Derived state is calculated from existing state rather than stored redundantly. `currentTrack` and `activeText` are examples.

### 34. Why avoid unnecessary duplicate state?
Duplicate state can get out of sync. This frontend mostly computes derived values instead of storing redundant copies.

### 35. What triggers a React re-render?
State changes, prop changes, and parent re-renders can all trigger rendering. That is why careful state design matters.

## Section 4: Routing and Navigation

### 36. What is client-side routing?
Client-side routing changes visible pages without reloading the browser. This project uses `react-router-dom` for routing.

### 37. Where is routing configured here?
`frontend/src/main.jsx` sets up `BrowserRouter`, and `frontend/src/App.jsx` defines the route table.

### 38. What is `BrowserRouter`?
It enables history-based routing in React Router. This app wraps `App` with `BrowserRouter`.

### 39. What are `Routes` and `Route`?
They define route matching and which component should render for a URL path. `App.jsx` uses both.

### 40. What is `Navigate`?
`Navigate` programmatically redirects from one route to another in the render tree. The app uses it for protection and fallback paths.

### 41. What is a protected route?
A protected route requires authentication before rendering the page. `ProtectedRoute` in `App.jsx` checks for a token in `localStorage`.

### 42. Why is route protection useful here?
The upload page should only be visible when the user is authenticated.

### 43. What is `useNavigate`?
`useNavigate` returns a function used to navigate programmatically. Login, register, reset-password, auth-callback, and upload flows use it.

### 44. What is `useLocation`?
`useLocation` exposes the current route location. This codebase uses it in `App.jsx` and auth-related flows.

### 45. Why is `useLocation` useful in this app?
It supports route-aware UI behavior, such as hiding the music player on certain screens or reading callback query state.

## Section 5: Forms and User Input

### 46. How are forms handled in this frontend?
They are handled with component state, event handlers, async submissions, and conditional messages for loading/success/error.

### 47. Why are login and registration pages good React examples?
They show controlled inputs, submit handling, async API calls, navigation, and error-state management.

### 48. What is form submission handling in React?
It is managing user input, preventing default behavior if needed, and sending structured data to an API. This happens in the auth and upload pages.

### 49. What is `FormData` and where is it used?
`FormData` is used for multipart form submissions. `UploadPage` uses it to send the PDF file to the backend upload endpoint.

### 50. Why use `FormData` here?
Because file uploads require multipart data rather than plain JSON.

### 51. How are errors shown to users in this app?
Components keep error messages in state and render them conditionally.

### 52. Why track loading state in forms?
It prevents duplicate submissions and gives feedback during async work. Pages use flags like `isLoading`, `isRegistering`, `isUploading`, and `isAsking`.

### 53. Why clear or normalize inputs after auth/callback actions?
It avoids stale or invalid tokens and keeps the UI predictable.

### 54. How is token storage handled in the frontend?
The frontend stores the auth token in `localStorage` and normalizes it before use in API requests.

### 55. What is the tradeoff of using `localStorage`?
It is simple and persistent across refreshes, but it must be handled carefully because it is browser-accessible.

## Section 6: API Calls and Frontend Architecture

### 56. How does the frontend call the backend?
It uses Axios through a shared client in `frontend/src/api/client.js`.

### 57. Why create a shared Axios client?
It centralizes base URL, auth headers, cache-control headers, and request interception logic.

### 58. What is an Axios interceptor?
An interceptor modifies requests or responses globally. This codebase uses a request interceptor to attach auth tokens and no-cache headers.

### 59. Why skip the auth header for `/auth/` requests?
Because login/register/reset endpoints should not reuse a stale token automatically.

### 60. What is `import.meta.env`?
It is Vite’s way to expose environment variables to frontend code. `client.js` reads `VITE_API_BASE_URL` through it.

### 61. Why is environment-based API configuration useful?
It allows local, Docker, and deployed environments to use different API base URLs without code changes.

### 62. Why add cache-control headers in the API client?
This frontend wants fresh data for document status and authenticated calls, so it explicitly disables caching.

### 63. Why add a timestamp parameter to GET requests?
It helps avoid stale cached responses, especially for polling-like status calls.

### 64. Why normalize bearer tokens on the client?
It prevents malformed headers like repeated `Bearer Bearer ...` from being sent.

### 65. What is separation of concerns in the frontend here?
Routing is in `App.jsx`, bootstrapping is in `main.jsx`, network concerns are in `api/client.js`, and page-specific behavior lives in page components.

## Section 7: React Rendering, Performance, and UX

### 66. What is declarative UI?
In React, you describe what the UI should look like for the current state. Components in this project follow that approach.

### 67. Why is declarative UI better than manual DOM manipulation?
It keeps code easier to reason about, especially when screens have many interactive states like the upload workflow.

### 68. How does the upload flow show practical React state modeling?
It breaks the experience into steps and reflects processing state through state variables instead of imperative DOM updates.

### 69. Why use refs instead of state for request counters?
Because request IDs are mutable control values and should not trigger re-renders.

### 70. Why is route-level code organization helpful?
It keeps large screens like upload, login, and registration focused and easier to maintain.

### 71. What is a side effect related to browser APIs?
Examples include reading/writing `localStorage`, audio playback control, DOM focus, and timers. This app performs all of those.

### 72. Why are timers and effects tricky in React?
Because they need cleanup and correct dependency handling. Components like `HeaderTypeTicker` and `FloatingMusicPlayer` show this kind of logic.

### 73. What does React mean by one-way data flow?
Data flows down from parent to child via props, while user events update local state. This app follows that standard pattern.

### 74. Why are fallback routes important?
They handle unknown URLs gracefully. `App.jsx` redirects unmatched paths to `/error`.

### 75. Why is UX state management important in interview discussions?
Because real frontend work is not just rendering static markup; it is managing async operations, errors, route changes, and user feedback cleanly.

## Section 8: Node Basics in the Context of This Repo

### 76. What is Node.js?
Node.js is a JavaScript runtime built on V8. In this repo it is used to run frontend tooling such as npm scripts and Vite.

### 77. Is Node used as the backend server in this project?
No. The backend server is Spring Boot. Node is used only for frontend development and build tooling.

### 78. Why is Node still important here?
Because the React frontend depends on Node for dependency installation, local dev server execution, and production build generation.

### 79. What is npm?
npm is the Node package manager. This repo uses it to install frontend dependencies and run scripts like `npm run dev` and `npm run build`.

### 80. Where is npm usage defined?
In `frontend/package.json` under the `scripts` section.

### 81. What is `package.json`?
It defines the frontend package metadata, scripts, dependencies, and devDependencies.

### 82. What are dependencies vs devDependencies?
Dependencies are needed by the app code at runtime/build bundle level, while devDependencies support development/build tooling. Here `react`, `react-dom`, `axios`, and `react-router-dom` are dependencies, while `vite` and `@vitejs/plugin-react` are devDependencies.

### 83. What does `type: module` mean in `package.json`?
It tells Node to treat files as ES modules by default. That aligns with modern Vite-based frontend development.

### 84. What are npm scripts?
They are named commands in `package.json`. This project defines `dev`, `build`, and `preview` scripts.

### 85. What does `npm run dev` do here?
It starts the Vite development server for the React frontend.

### 86. What does `npm run build` do here?
It builds the production-ready frontend bundle using Vite.

### 87. What does `npm run preview` do here?
It runs a local preview server for the built frontend output.

### 88. What is Vite?
Vite is the frontend build tool and dev server used in this project. It provides fast local development and optimized production builds.

### 89. Why is Vite a good fit for this repo?
It is lightweight, fast, and works well with modern React and ES modules.

### 90. What does `import.meta.env` have to do with Node/Vite?
It is provided by the Vite toolchain, which runs on Node during development/build time.

### 91. What is a module in JavaScript/Node terms?
A module is a file that exports and imports code. This frontend uses ES module syntax such as `import` and `export default`.

### 92. How are modules used in this frontend?
Components, pages, and shared utilities import each other cleanly using ES module syntax.

### 93. What is the purpose of the `node_modules` directory?
It stores installed packages. The frontend depends on packages like React, Axios, React Router, and Vite from there.

### 94. Why should you not hand-edit `node_modules`?
Because it is generated dependency output, not source code, and will be replaced by reinstalling packages.

### 95. What is a lockfile and why is it useful?
The lockfile pins exact dependency versions for consistent installs. This repo includes `frontend/package-lock.json`.

### 96. Why is reproducible dependency installation important?
It reduces “works on my machine” issues and keeps the build stable across environments.

### 97. What is the difference between the browser runtime and Node runtime?
React UI code runs in the browser, while tooling like Vite and npm scripts run in Node. This repo uses both runtimes for different purposes.

### 98. Why is it important not to confuse Node tooling with a Node backend?
Because interviewers will notice the difference. In this repo, business APIs are served by Spring Boot, not Node.

### 99. How should you explain Node experience from this codebase honestly?
Say that you used Node for frontend package management, local development workflow, environment-driven builds, and Vite-based bundling rather than for backend API implementation.

### 100. If asked “How do React and Node work together in this project?”, what should you say?
You can say that React provides the UI component model, routing, and browser-side interaction logic, while Node powers the frontend development/build toolchain through npm and Vite. The produced frontend then communicates with the Spring Boot backend over HTTP.

## Strong Interview Summary

If you need a short summary, use this:

"In this project, React is used to build the frontend through functional components, hooks, routing, controlled forms, shared API client logic, and browser-side auth state management. Node is used as the JavaScript runtime for the frontend toolchain, including npm dependency management and Vite-based development and production builds. The key point is that React powers the UI, Node powers the build/runtime tooling for the frontend, and Spring Boot serves the backend APIs."

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
