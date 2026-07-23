# Top 25 Node Interview Questions Cheat Sheet

Date: 2026-03-26
Project: PolicyMind Document Service

## Important Context

This repo uses Node for frontend tooling, not as the backend API server. The backend server is Spring Boot. Say this explicitly and early in an interview — it's a common (incorrect) assumption that a `frontend/package.json` implies a Node backend exists somewhere, and correcting it proactively signals you understand what Node actually is (a JavaScript runtime) rather than treating it as a synonym for "backend."

### 1. What is Node.js?
Node.js is a JavaScript runtime built on Chrome's V8 engine, letting JS run outside a browser. In this project it exclusively runs frontend build/dev tooling — npm and Vite — never as a request-handling server.

### 2. Is Node the backend here?
No. Spring Boot (Java, under `src/main/java/com/policymind/document/`) is the backend. Node's only role is running the frontend's build/dev tooling — there is no Express/NestJS/Fastify server anywhere in this repo.

### 3. What is npm?
npm is Node's package manager, used to install frontend dependencies (`react`, `axios`, `react-router-dom`, ...) and to run the project's defined scripts (`npm run dev`, `npm run build`, `npm run preview`).

### 4. Where is npm configured?
In `frontend/package.json`, which defines the dependency list, devDependency list, and the `scripts` section that names each runnable command.

### 5. What is `package.json`?
The manifest file for a Node package — it declares metadata (name, `"type": "module"`), the `scripts` map, and the `dependencies`/`devDependencies` this frontend needs.

### 6. What are npm scripts?
Named shell commands under `"scripts"` in `package.json`, run via `npm run <name>`. This project defines exactly three: `"dev": "vite"`, `"build": "vite build"`, `"preview": "vite preview"`.

### 7. What does `npm run dev` do here?
Starts Vite's development server with hot module replacement, so edits to `frontend/src/` show up in the browser almost instantly without a full reload — this is the command used for local development.

### 8. What does `npm run build` do here?
Runs `vite build`, producing the optimized, minified static production bundle — the actual artifact copied into the frontend's Docker image and served by nginx, distinct from the unbundled dev-server output.

### 9. What does `npm run preview` do here?
Runs `vite preview`, serving the already-built production bundle locally, so you can verify the real production output rather than the dev server's live-source version before deploying.

### 10. What is Vite?
The build tool and dev server this project uses — it serves source natively over ESM during development (fast startup, no full-bundle rebuild per change) and uses Rollup for optimized production builds.

### 11. Why use Vite?
It's fast in both dev (near-instant server start, quick hot updates) and production (tree-shaken Rollup output), with low-configuration first-class React support via `@vitejs/plugin-react` — a good fit for a frontend this size without heavier webpack-style configuration overhead.

### 12. What is a dependency?
A package the running application actually needs, bundled into the production output. Here: `react ^18.3.1`, `react-dom ^18.3.1`, `axios ^1.8.3`, `react-router-dom ^6.30.1`.

### 13. What is a devDependency?
A package needed only for development/build tooling, never shipped in the production bundle. Here: `vite ^5.4.14` and `@vitejs/plugin-react ^4.4.1`.

### 14. What does `type: module` mean?
It tells Node to treat this package's `.js` files as native ES modules (`import`/`export`) by default instead of CommonJS (`require`) — matching how the whole frontend, and Vite itself, is written.

### 15. What is an ES module?
A JavaScript file that explicitly `import`s and `export`s values, giving it its own scope rather than relying on shared globals. This frontend uses ES module syntax throughout — e.g. `import axios from "axios"` in `frontend/src/api/client.js`.

### 16. What is `node_modules`?
The directory npm installs every direct and transitive dependency into, resolved from `package.json`/`package-lock.json`. Vite resolves imports like `import axios from "axios"` against its contents at both dev and build time.

### 17. Why shouldn't you edit `node_modules`?
It's entirely generated, disposable output — `npm install` regenerates it from the manifest and lockfile at any time, silently discarding manual edits, and it's git-ignored, so edits wouldn't even be shared with the rest of the team.

### 18. What is a lockfile?
`frontend/package-lock.json` — it pins the exact resolved version of every dependency and transitive dependency, not just the version ranges (`^18.3.1`) declared in `package.json`.

### 19. Why is a lockfile important?
Without it, two `npm install` runs on the same `package.json` could resolve slightly different transitive dependency versions over time. The lockfile guarantees a developer's machine, CI, and a Docker build all resolve the identical dependency tree — CI typically enforces this with `npm ci`, which fails outright if the lockfile is out of sync.

### 20. What is the difference between browser JavaScript and Node JavaScript here?
The actual React app (`App.jsx`, every page/component) runs in the end user's browser against browser APIs (DOM, `localStorage`, `<audio>`). npm and Vite run in Node, using filesystem access to read source and produce the bundle the browser will later execute — two runtimes, two very different jobs, both present in this one repo.

### 21. What is `import.meta.env`?
Vite's browser-side API for build-time environment variables, populated by Vite (running on Node) reading `.env` files and injecting `VITE_`-prefixed values into the client bundle. `frontend/src/api/client.js` reads `import.meta.env.VITE_API_BASE_URL`, falling back to `"/api"`.

### 22. Why is environment-based configuration useful?
It lets the same frontend build point at different backend URLs — local dev, Docker Compose, deployed AWS/EC2 — purely by changing the `VITE_API_BASE_URL` environment variable at build time, with zero source code changes between environments.

### 23. How should you describe Node experience from this repo honestly?
"I used Node for frontend package management via npm, the local development workflow via Vite's dev server, and environment-driven, ES-module-based production builds — not for implementing backend APIs, since that's entirely handled by Spring Boot in this project."

### 24. How do React and Node work together here?
React provides the UI code that runs in the browser; Node powers the toolchain (npm + Vite) that installs dependencies, serves that code during development, and bundles it for production. The built frontend then talks to the Spring Boot backend over plain HTTP — Node itself never handles any of those requests.

### 25. What is the best short interview summary?
"In this project, Node is the runtime behind npm and Vite for the frontend's dependency management, dev server, and production build — it never runs as an application server. The actual backend APIs are served by Spring Boot, written in Java."

## Best Files to Review Fast

- `frontend/package.json`
- `frontend/src/api/client.js`
- `README.md`
