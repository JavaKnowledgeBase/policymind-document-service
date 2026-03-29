# Top 25 React Interview Questions Cheat Sheet

Date: 2026-03-26
Project: PolicyMind Document Service

## How to Use This

These are the 25 React questions most likely to come up first in an interview, answered using examples from this codebase.

### 1. What is React?
React is the UI library used in `frontend/src/` to build the application screens and reusable components.

### 2. What is a functional component?
A component written as a function. This frontend uses functional components throughout, such as `LoginPage`, `UploadPage`, and `FloatingMusicPlayer`.

### 3. What is JSX?
JSX is the syntax used to describe UI in JavaScript. Files like `frontend/src/App.jsx` use JSX heavily.

### 4. What is `createRoot`?
React 18 API for mounting the app. Used in `frontend/src/main.jsx`.

### 5. What is `StrictMode`?
A development-only helper that catches unsafe patterns. Enabled in `frontend/src/main.jsx`.

### 6. What are props?
Inputs passed into components. `ProtectedRoute({ children })` in `App.jsx` is a simple example.

### 7. What is `children`?
A special prop representing nested UI passed into a component.

### 8. What is state?
State is data that changes over time and triggers re-rendering. `useState` is used across the frontend for forms and UI flow.

### 9. What is `useState`?
Hook for local component state. Used in login, registration, upload, reset password, and component-level UI behavior.

### 10. What is `useEffect`?
Hook for side effects such as async actions, timers, navigation effects, and browser interactions.

### 11. Where is `useEffect` used here?
In `LoginPage`, `RegisterPage`, `UploadPage`, `AuthCallbackPage`, `FloatingMusicPlayer`, and `HeaderTypeTicker`.

### 12. What is `useRef`?
Hook for mutable values or DOM references without causing re-renders. `UploadPage` uses refs for request tracking and scrolling/focus logic.

### 13. What is `useMemo`?
Hook for memoized derived values. Used in `HeaderTypeTicker` and `FloatingMusicPlayer`.

### 14. What is `useCallback`?
Hook for memoizing function references. Used in `FloatingMusicPlayer` for audio handlers.

### 15. What is conditional rendering?
Rendering different UI based on conditions. `App.jsx` hides the music player on auth routes and protects the upload page.

### 16. What is routing in React?
Client-side navigation handled without full page reloads. This app uses `react-router-dom`.

### 17. What are `BrowserRouter`, `Routes`, and `Route`?
They define and run the route system. Configured in `main.jsx` and `App.jsx`.

### 18. What is `Navigate`?
A redirect component used for route fallback and protected navigation.

### 19. What is a protected route?
A route that only renders when authenticated. `ProtectedRoute` checks `localStorage` for a token.

### 20. What is a controlled form?
A form where input values live in React state. Login, register, and reset-password pages use this pattern.

### 21. How does this frontend call the backend?
Using Axios through the shared client in `frontend/src/api/client.js`.

### 22. Why use a shared API client?
It centralizes base URL config, auth token handling, cache headers, and interceptor logic.

### 23. What is an interceptor?
A function that modifies requests or responses globally. This frontend uses a request interceptor to attach JWT auth headers.

### 24. How is auth state handled in the frontend?
The token is stored in `localStorage`, normalized in the API client, and checked in route protection.

### 25. How would you summarize your React work from this project?
I built a React frontend using functional components, hooks, client-side routing, controlled forms, a shared Axios API layer, and browser-side auth state, all connected to a Spring Boot backend.

## Best Files to Review Fast

- `frontend/src/main.jsx`
- `frontend/src/App.jsx`
- `frontend/src/api/client.js`
- `frontend/src/pages/LoginPage.jsx`
- `frontend/src/pages/UploadPage.jsx`
- `frontend/src/components/FloatingMusicPlayer.jsx`
