# Full Stack Mock Interview Script

Date: 2026-03-26
Project: PolicyMind Document Service
Audience: Practice interviews for Java, Spring Boot, React, Node, and architecture

## How to Use This

Read the interviewer prompt first, then answer in your own words. The model answer is here to help you tighten your explanation.

### 1. Give me a quick overview of this project.
Model answer:
This project is a document-processing and question-answering application. The backend is a Spring Boot service that handles authentication, PDF upload, async document processing, chunk generation, embeddings, and question answering with OpenAI or Vertex AI. The frontend is built with React and talks to the backend over HTTP. PostgreSQL stores users, documents, and chunks, while Redis supports runtime features.

### 2. Is this a microservices project?
Model answer:
It is more accurate to call it a microservice-style backend than a full multi-service microservices platform. This repo contains one Spring Boot business service plus a React frontend and infrastructure services. It uses microservice-friendly patterns such as externalized config, containerization, async processing, and isolated external integrations.

### 3. How is Java used in this project?
Model answer:
Java is used for the backend service implementation. Core Java concepts show up in services, entities, repositories, custom exceptions, records, collections, streams, async processing, and security filters.

### 4. How is Spring Boot used here?
Model answer:
Spring Boot is the application framework for the backend. It bootstraps the service, provides dependency injection, config management, web APIs, JPA integration, security, and async execution.

### 5. Explain the layered backend architecture.
Model answer:
Controllers expose HTTP APIs, services hold business and orchestration logic, repositories handle persistence through JPA, entities/model classes represent stored data, and the security package manages JWT and OAuth2 concerns.

### 6. Walk me through the document upload flow.
Model answer:
The frontend uploads a PDF to `/upload`. The controller delegates to `DocumentService`, which validates the file, creates a document record, and hands it off to an async worker. The worker invokes the processing pipeline, which extracts text, chunks it, generates embeddings, stores the chunks, and marks processing complete or failed.

### 7. Why did you make upload asynchronous?
Model answer:
PDF parsing and embedding generation can be slow, so returning `202 Accepted` avoids blocking the request thread and improves user-perceived responsiveness.

### 8. How does the question-answering flow work?
Model answer:
Stored chunks are loaded from the database, the question is embedded, chunk similarity is computed, top chunks are used as context, and that context is sent to OpenAI, Vertex, or both to generate a structured answer.

### 9. How is authentication handled?
Model answer:
The backend uses Spring Security with JWT-based auth and OAuth2 login support. A JWT authentication filter validates tokens, and the security config declares public and protected routes.

### 10. What database design does the app use?
Model answer:
PostgreSQL stores users, document metadata, and document chunks. JPA entities model this state, and Spring Data repositories abstract CRUD operations.

### 11. Where does React fit into the system?
Model answer:
React implements the frontend UI: login, registration, password reset, upload workflow, architecture page, and auth callback handling.

### 12. How is routing implemented on the frontend?
Model answer:
Routing is handled by `react-router-dom` using `BrowserRouter`, `Routes`, `Route`, `Navigate`, and a simple protected-route wrapper.

### 13. How does the frontend call the backend?
Model answer:
Through a shared Axios client that centralizes the API base URL, auth token handling, request headers, and cache-busting behavior.

### 14. How is frontend auth state managed?
Model answer:
The frontend stores the JWT token in `localStorage`, reads it in the Axios client, and checks for it inside route protection logic.

### 15. What is Node used for in this repo?
Model answer:
Node is used for frontend tooling only. It runs npm scripts and the Vite dev/build toolchain. The backend itself is not a Node server.

### 16. How would you explain your Node experience honestly from this repo?
Model answer:
I used Node for frontend dependency management, local development, module-based builds, and Vite-based bundling rather than backend API development.

### 17. What Java fundamentals can you point to here?
Model answer:
Constructor injection, OOP, enums, inheritance in security filters, collections, generics, streams, records, exception handling, and async execution are all visible in the backend.

### 18. What React fundamentals can you point to here?
Model answer:
Functional components, hooks like `useState`, `useEffect`, `useRef`, `useMemo`, `useCallback`, controlled forms, routing, conditional rendering, and shared API abstraction.

### 19. What backend reliability work did you implement?
Model answer:
We added shared outbound resilience for downstream services: timeouts, retries, circuit breakers, bulkheads, and safe fallbacks where appropriate.

### 20. Why didn’t you use fallback everywhere?
Model answer:
Because fallback is only appropriate when degraded output is still safe. AI text can fall back to a structured degraded response, but reCAPTCHA should fail closed and embeddings should not use fake vectors.

### 21. What are the most important design tradeoffs in this system?
Model answer:
The biggest tradeoff is that async processing is currently in-process rather than a separate worker service. That keeps implementation simpler now, but a queue-backed worker split would be stronger at larger scale.

### 22. How would you scale this further?
Model answer:
I would extract the processing worker into its own service, add a queue, move toward explicit job orchestration, and possibly introduce a dedicated vector store if retrieval scale or query sophistication increased.

### 23. What would you highlight as senior-level decisions?
Model answer:
Clear service boundaries, externalized configuration, defensive integration handling, async processing, realistic fallback strategy, and being precise about the current architecture instead of overstating it.

### 24. How do you talk about this project without exaggerating it?
Model answer:
I say it is a Spring Boot backend built with microservice-oriented patterns, not a full distributed Spring Cloud platform. That is accurate and technically credible.

### 25. Give me your final 60-second summary.
Model answer:
This project is a full-stack document intelligence application. The backend is a Spring Boot service that handles auth, upload, async processing, chunking, embeddings, retrieval, and AI-assisted answers using OpenAI or Vertex. The frontend is built in React with modern hook-based components, routing, controlled forms, and a shared Axios client. PostgreSQL stores application data, Redis supports runtime features, and Docker Compose wires the system together. I also added downstream resilience patterns so the service behaves better under real integration failures.

## Practice Advice

For best results, practice answering each question twice:
- once in a short 20-second version
- once in a deeper 60-90 second version

That will make you much more flexible in real interviews.
