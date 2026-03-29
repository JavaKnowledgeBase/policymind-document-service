# Spring Boot Microservice Architecture Guide

Date: 2026-03-26
Project: PolicyMind Document Service
Audience: Senior developer / architecture walkthrough / interview explanation

## Executive Summary

This project implements a Spring Boot backend as the core business service for PolicyMind. It is best described as a microservice-style backend rather than a full multi-service Spring microservices platform.

What is present in this repository:
- one Spring Boot backend service
- one React frontend
- PostgreSQL for persistence
- Redis for supporting cached/runtime features
- external integrations for OpenAI, Vertex AI, OAuth providers, and reCAPTCHA

What is not present in this repository:
- multiple independent Spring Boot services owned in the same codebase
- service discovery
- API gateway built as a separate Spring Cloud service
- distributed messaging between multiple internal services
- centralized config server or service registry

So the most accurate explanation is:

"This project contains a single Spring Boot business service that is built using microservice-friendly patterns such as stateless HTTP APIs, externalized configuration, containerized deployment, asynchronous processing, isolated infrastructure dependencies, and explicit downstream integrations."

## Runtime Architecture

At runtime, the system consists of these major parts:

1. Frontend
- React + Vite UI in `frontend/`
- Talks to the backend over HTTP

2. Spring Boot backend
- Main business service in `src/main/java/com/policymind/document/`
- Exposes REST endpoints for auth, upload, document status, content, and question answering

3. PostgreSQL
- Primary system of record
- Stores users, document metadata, and document chunks

4. Redis
- Supporting infrastructure dependency
- Used by Redis-backed features such as content-serving/caching support

5. External services
- OpenAI
- Vertex AI
- Google reCAPTCHA
- OAuth providers such as Google, Microsoft, Facebook, LinkedIn, Twitter/X

## How the Spring Boot Service Is Bootstrapped

File:
- `src/main/java/com/policymind/document/DocumentServiceApplication.java`

Key annotations:
- `@SpringBootApplication`
- `@EnableAsync`

What this means:
- `@SpringBootApplication` enables component scanning, auto-configuration, and Spring Boot startup wiring.
- `@EnableAsync` enables background execution for annotated methods, which this project uses for document processing.

Why this matters architecturally:
- The service starts as a standard Spring Boot application.
- The backend is self-contained and container-friendly.
- Async support allows long-running document work to be decoupled from the upload request lifecycle.

## Deployment Model

Primary deployment descriptor:
- `docker-compose.yml`

Services defined there:
- `postgres`
- `redis`
- `policymind` (the Spring Boot backend)
- `frontend`

This is important because it shows the backend is deployed as an independently runnable service container with separate infrastructure dependencies.

That is aligned with microservice deployment principles even though the repo itself only contains one Spring Boot business service.

## Spring Boot Layering in This Project

The backend follows a conventional layered Spring architecture.

### 1. Controller layer

Representative files:
- `src/main/java/com/policymind/document/controller/DocumentController.java`
- `src/main/java/com/policymind/document/controller/HealthController.java`
- `src/main/java/com/policymind/document/controller/HeaderContentController.java`
- `src/main/java/com/policymind/document/security/AuthController.java`

Responsibilities:
- define HTTP endpoints
- validate request presence/shape at the boundary
- delegate business work to services
- map exceptions to HTTP responses
- preserve API-level concerns like cache headers and response status codes

Example patterns in this project:
- `/upload` accepts a multipart file and returns `202 Accepted`
- `/documents/{id}` exposes polling-friendly status retrieval
- `/auth/**` manages registration, password login, forgot-password, and reset flows

Why this is microservice-appropriate:
- controllers stay thin and transport-focused
- the HTTP API is the contract boundary for clients
- business logic is not embedded in controllers

### 2. Service layer

Representative files:
- `src/main/java/com/policymind/document/service/DocumentService.java`
- `src/main/java/com/policymind/document/service/DocumentProcessingPipeline.java`
- `src/main/java/com/policymind/document/service/DocumentProcessingWorker.java`
- `src/main/java/com/policymind/document/service/AuthService.java`
- `src/main/java/com/policymind/document/service/EmbeddingService.java`
- `src/main/java/com/policymind/document/service/OpenAiService.java`
- `src/main/java/com/policymind/document/service/VertexAiService.java`
- `src/main/java/com/policymind/document/service/RecaptchaService.java`
- `src/main/java/com/policymind/document/service/PdfService.java`
- `src/main/java/com/policymind/document/service/ChunkService.java`

Responsibilities:
- implement business workflows
- orchestrate persistence and downstream integrations
- encapsulate provider-specific logic
- separate synchronous API handling from async processing

This is the main domain and orchestration layer of the backend.

### 3. Repository layer

Representative files:
- `src/main/java/com/policymind/document/repository/DocumentRepository.java`
- `src/main/java/com/policymind/document/repository/DocumentChunkRepository.java`
- `src/main/java/com/policymind/document/repository/UserRepository.java`

Responsibilities:
- provide persistence access via Spring Data JPA
- abstract database CRUD and query concerns

Why this matters:
- the service layer depends on repository interfaces, not direct SQL scattered through business code
- persistence concerns are cleanly isolated

### 4. Entity/model layer

Representative files:
- `src/main/java/com/policymind/document/entity/User.java`
- `src/main/java/com/policymind/document/entity/DocumentChunk.java`
- `src/main/java/com/policymind/document/model/Document.java`

Responsibilities:
- represent persistent domain state
- map Java objects to relational tables using JPA annotations

### 5. Security layer

Representative files:
- `src/main/java/com/policymind/document/security/SecurityConfig.java`
- `src/main/java/com/policymind/document/security/JwtAuthenticationFilter.java`
- `src/main/java/com/policymind/document/security/JwtService.java`
- `src/main/java/com/policymind/document/security/CustomOAuth2UserService.java`
- `src/main/java/com/policymind/document/security/OAuth2AuthenticationSuccessHandler.java`
- `src/main/java/com/policymind/document/security/OAuth2AuthenticationFailureHandler.java`

Responsibilities:
- configure JWT authentication
- configure OAuth2 login support
- define public versus protected routes
- configure CORS and security filters

Why this is important:
- security is implemented as a first-class infrastructural layer, not embedded ad hoc into endpoints

## Request Flow: Authentication

Main entrypoint:
- `AuthController`

How it works:
1. Client calls `/auth/register`, `/auth/login/password`, `/auth/forgot-password/question`, or `/auth/forgot-password/reset`
2. `AuthController` delegates to `AuthService`
3. `AuthService` interacts with `UserRepository`, password encoding, JWT generation, and reCAPTCHA verification
4. Security state is enforced through Spring Security and JWT filtering

Supporting Spring features used:
- `@RestController`
- constructor injection
- `PasswordEncoder` bean
- Spring Security filter chain
- JWT filter before `UsernamePasswordAuthenticationFilter`

Why this is good architecture:
- controllers are transport-only
- auth logic is centralized in a dedicated service
- token validation is handled through filter-based infrastructure
- OAuth2 integration uses Spring Security rather than custom low-level code

## Request Flow: Document Upload and Processing

Main entrypoint:
- `DocumentController`

How it works:
1. Client uploads a PDF to `POST /upload`
2. `DocumentController` delegates to `DocumentService.submitDocument(...)`
3. `DocumentService` validates the file, creates a `Document` record with `QUEUED` status, and reads file bytes
4. `DocumentService` calls `DocumentProcessingWorker.processDocumentAsync(...)`
5. `DocumentProcessingWorker` runs asynchronously because of `@Async`
6. `DocumentProcessingPipeline` performs the actual processing workflow:
   - mark status as `PROCESSING`
   - extract text from PDF
   - split text into chunks
   - generate embeddings for each chunk
   - store chunks and embeddings
   - mark document as `COMPLETED` or `FAILED`
7. Client polls `GET /documents/{id}` for state

Why this is architecturally significant:
- upload request latency is decoupled from document processing latency
- the API returns quickly with `202 Accepted`
- the backend behaves more like a scalable worker-backed service even though the worker is still in-process

Senior-level nuance:
- this is not yet a separate worker microservice
- it is, however, clearly structured in a way that could later be extracted into one

## Request Flow: Question Answering over Uploaded Documents

Main entrypoint:
- `POST /{id}/ask` in `DocumentController`

How it works:
1. User asks a question about a previously processed document
2. `DocumentService.askQuestion(...)` loads stored chunks from the database
3. The question is embedded using `EmbeddingService`
4. Stored chunk embeddings are deserialized and ranked by cosine similarity
5. Top chunks become the retrieval context
6. The context is sent to either:
   - `OpenAiService`
   - `VertexAiService`
   - or both
7. Structured output is returned to the caller

Why this matters:
- the service is implementing a retrieval-augmented generation style backend
- AI provider logic is isolated behind service classes
- the orchestration remains in a domain service rather than in the controller

## How Spring Security Is Implemented

File:
- `src/main/java/com/policymind/document/security/SecurityConfig.java`

Key implementation details:
- `SecurityFilterChain` defines route authorization rules
- `/`, `/health`, `/auth/**`, `/oauth2/**`, `/login/**`, and `/content/**` are public
- all other routes require authentication
- JWT authentication filter is inserted before `UsernamePasswordAuthenticationFilter`
- OAuth2 login is configured using Spring Security
- CORS policy is configured through `CorsConfigurationSource`
- session strategy is controlled with `SessionCreationPolicy.IF_REQUIRED`

Why this is a strong Spring Boot implementation:
- security is configured declaratively
- authentication is filter-based and framework-native
- CORS and OAuth2 are handled at the framework layer
- protected APIs remain clean because the security contract is externalized

## Persistence Implementation

Primary database:
- PostgreSQL

Spring persistence technology:
- Spring Data JPA
- Hibernate through Spring Boot auto-configuration

Configured in:
- `src/main/resources/application.yml`
- `docker-compose.yml`

How it works:
- datasource properties are externalized
- repositories extend `JpaRepository`
- entities/models are persisted automatically by JPA/Hibernate
- document and chunk lifecycle state is stored relationally

Why this is suitable:
- structured document metadata and chunk records are naturally relational
- JPA speeds up CRUD and repository implementation
- persistence is clearly separated from orchestration logic

## Configuration Model

Main config file:
- `src/main/resources/application.yml`

Configuration style used:
- Spring Boot externalized configuration
- environment variable substitution for secrets and deployment differences

Examples:
- DB connection settings
- OAuth client settings
- OpenAI key
- GCP/Vertex settings
- JWT secret
- multipart upload limits
- reCAPTCHA settings
- network resilience settings

Why this is microservice-friendly:
- no hardcoded environment-specific values
- containers can be promoted between environments
- the service is deployable with environment-based configuration overrides

## Async Processing Model

Files:
- `src/main/java/com/policymind/document/DocumentServiceApplication.java`
- `src/main/java/com/policymind/document/service/DocumentProcessingWorker.java`

How it works:
- async support is enabled globally with `@EnableAsync`
- worker method is annotated with `@Async`
- upload requests enqueue in-process background work instead of blocking until completion

Why this matters:
- better user-perceived responsiveness
- clear separation between API acceptance and heavy processing
- positions the service for future extraction into a queue-backed worker model

Architectural truth to state clearly:
- today this is async processing within one Spring Boot application
- it is not yet a separate processing microservice

## External Integration Pattern

The Spring Boot backend integrates with multiple external systems through dedicated service classes.

Examples:
- `OpenAiService`
- `VertexAiService`
- `RecaptchaService`
- `CustomOAuth2UserService`

Why this is a good Spring microservice pattern:
- external providers are isolated behind clear service boundaries
- provider-specific logic is not leaked into controllers
- future provider substitutions are easier
- resilience and timeout policies can be applied at those boundaries

## Why This Project Can Be Explained as “Microservice-Oriented”

It is reasonable to describe the backend as microservice-oriented because it has these characteristics:
- independently deployable Spring Boot service
- stateless HTTP API design for core workflows
- externalized configuration
- isolated database and infrastructure dependencies
- containerized runtime
- async task handling
- explicit downstream integration boundaries
- security and transport handled via framework infrastructure

## Why It Should Not Be Overstated as a Full Spring Microservices Platform

To stay technically credible, also say this:

This repository does not currently implement a full distributed Spring Cloud microservices platform. It contains one main Spring Boot backend service, supported by a frontend and infrastructure services. Some internal boundaries, especially the async document-processing worker, are designed in a way that could be extracted into separate microservices later.

That is the strongest and most accurate senior-level explanation.

## Recommended Interview / Architecture Explanation

You can use this wording:

"In this project, the main business backend is implemented as a Spring Boot service with conventional layered architecture: controllers for HTTP APIs, services for orchestration and domain logic, repositories for persistence, entities for relational state, and a dedicated security layer using Spring Security, JWT, and OAuth2. It runs as an independently deployable container alongside PostgreSQL and Redis, and it integrates with external AI and identity providers through dedicated service boundaries. While the repo does not contain multiple Spring Boot microservices, the backend is implemented using microservice-friendly patterns such as externalized configuration, stateless API design, async processing, and isolated downstream integrations."

## Files to Review During a Walkthrough

Core bootstrapping:
- `src/main/java/com/policymind/document/DocumentServiceApplication.java`

Controllers:
- `src/main/java/com/policymind/document/controller/DocumentController.java`
- `src/main/java/com/policymind/document/security/AuthController.java`
- `src/main/java/com/policymind/document/controller/HealthController.java`
- `src/main/java/com/policymind/document/controller/HeaderContentController.java`

Services:
- `src/main/java/com/policymind/document/service/DocumentService.java`
- `src/main/java/com/policymind/document/service/DocumentProcessingPipeline.java`
- `src/main/java/com/policymind/document/service/DocumentProcessingWorker.java`
- `src/main/java/com/policymind/document/service/AuthService.java`
- `src/main/java/com/policymind/document/service/OpenAiService.java`
- `src/main/java/com/policymind/document/service/VertexAiService.java`
- `src/main/java/com/policymind/document/service/EmbeddingService.java`
- `src/main/java/com/policymind/document/service/RecaptchaService.java`
- `src/main/java/com/policymind/document/service/PdfService.java`
- `src/main/java/com/policymind/document/service/ChunkService.java`

Security:
- `src/main/java/com/policymind/document/security/SecurityConfig.java`
- `src/main/java/com/policymind/document/security/JwtAuthenticationFilter.java`
- `src/main/java/com/policymind/document/security/JwtService.java`
- `src/main/java/com/policymind/document/security/CustomOAuth2UserService.java`

Persistence:
- `src/main/java/com/policymind/document/repository/DocumentRepository.java`
- `src/main/java/com/policymind/document/repository/DocumentChunkRepository.java`
- `src/main/java/com/policymind/document/repository/UserRepository.java`
- `src/main/java/com/policymind/document/entity/User.java`
- `src/main/java/com/policymind/document/entity/DocumentChunk.java`
- `src/main/java/com/policymind/document/model/Document.java`

Config and runtime:
- `src/main/resources/application.yml`
- `docker-compose.yml`
- `README.md`
