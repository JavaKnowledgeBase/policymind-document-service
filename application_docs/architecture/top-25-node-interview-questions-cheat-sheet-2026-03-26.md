# Top 25 Node Interview Questions Cheat Sheet

Date: 2026-03-26
Project: PolicyMind Document Service

## Important Context

This repo uses Node for frontend tooling, not as the backend API server. The backend server is Spring Boot.

### 1. What is Node.js?
Node.js is a JavaScript runtime. In this project it runs npm and Vite tooling for the frontend.

### 2. Is Node the backend here?
No. Spring Boot is the backend. Node is used for frontend build/dev tooling.

### 3. What is npm?
npm is the package manager used to install frontend dependencies and run scripts.

### 4. Where is npm configured?
In `frontend/package.json`.

### 5. What is `package.json`?
It defines scripts, dependencies, and project metadata.

### 6. What are npm scripts?
Named commands in `package.json`. This project defines `dev`, `build`, and `preview`.

### 7. What does `npm run dev` do here?
Starts the Vite dev server.

### 8. What does `npm run build` do here?
Builds the production frontend bundle.

### 9. What does `npm run preview` do here?
Runs a preview server for the built frontend assets.

### 10. What is Vite?
Vite is the frontend dev server and bundler used in this repo.

### 11. Why use Vite?
It provides a fast local dev experience and modern production builds.

### 12. What is a dependency?
A package needed by the app, such as `react`, `axios`, and `react-router-dom`.

### 13. What is a devDependency?
A package needed for development/build tooling, such as `vite` and `@vitejs/plugin-react`.

### 14. What does `type: module` mean?
It enables ES module behavior by default in the frontend package.

### 15. What is an ES module?
A JavaScript module using `import` and `export`. This frontend uses ES modules throughout.

### 16. What is `node_modules`?
The installed package directory created by npm.

### 17. Why shouldn’t you edit `node_modules`?
Because it is generated dependency output and will be overwritten.

### 18. What is a lockfile?
A file that pins exact dependency versions. This repo has `frontend/package-lock.json`.

### 19. Why is a lockfile important?
It makes installs more reproducible across environments.

### 20. What is the difference between browser JavaScript and Node JavaScript here?
React code runs in the browser, while npm/Vite/build tools run in Node.

### 21. What is `import.meta.env`?
Vite-provided environment access for frontend code. Used in `frontend/src/api/client.js`.

### 22. Why is environment-based configuration useful?
It lets different environments use different API base URLs without changing source code.

### 23. How should you describe Node experience from this repo honestly?
I used Node for frontend package management, development workflow, module-based build tooling, and Vite-based bundling.

### 24. How do React and Node work together here?
React provides the UI code, while Node powers the build and dev toolchain that serves and bundles that UI.

### 25. What is the best short interview summary?
In this project, Node is the runtime behind npm and Vite for the frontend workflow, while the actual backend APIs are served by Spring Boot.

## Best Files to Review Fast

- `frontend/package.json`
- `frontend/src/api/client.js`
- `README.md`
