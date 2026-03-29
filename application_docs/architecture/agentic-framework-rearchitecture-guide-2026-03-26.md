# Agentic Framework Re-Architecture Guide

Date: 2026-03-26
Project: PolicyMind Document Service
Audience: Engineering, architecture, product, and implementation planning

## Executive Summary

PolicyMind already has the right foundations for an agentic system:

- a clear ingestion pipeline
- explicit document and chunk persistence
- retrieval-backed question answering
- asynchronous processing boundaries
- isolated integrations for OpenAI and Vertex AI

Today, however, the platform is still mostly a deterministic pipeline with a single in-process async worker:

- `DocumentController` accepts uploads and question requests
- `DocumentService` orchestrates uploads, status checks, and Q&A
- `DocumentProcessingWorker` is an in-process `@Async` worker
- `DocumentProcessingPipeline` extracts text, chunks content, generates embeddings, and persists results
- `DocumentChunkRepository` and `DocumentRepository` store state in PostgreSQL

That means the system is good at:

- ingesting PDFs
- generating embeddings
- retrieving relevant chunks
- asking a model to answer from retrieved context

But it is still limited when the user problem requires:

- multi-step reasoning
- iterative retrieval
- document-type-specific workflows
- tool selection
- confidence gating
- validation before returning final output

The right re-architecture is not "make everything agentic." The right move is:

1. Keep deterministic ingestion and storage as normal services.
2. Add an agent orchestration layer for complex analysis and workflow selection.
3. Introduce explicit agent state, task tracking, and tool contracts.
4. Move heavy background execution from in-process async calls to queue-backed workers.

That gives PolicyMind a hybrid architecture:

- deterministic services for ingestion, persistence, security, and repeatable transformations
- agentic workflows for reasoning, orchestration, validation, and adaptive retrieval

That hybrid model is the safest and most scalable fit for this repository.

## What the Current Project Is Doing Well

After reviewing the codebase, the current backend already has several strong boundaries that an agentic design can build on.

### Existing strengths

- `src/main/java/com/policymind/document/controller/DocumentController.java`
  - clean HTTP boundary for upload, status polling, and Q&A
- `src/main/java/com/policymind/document/service/DocumentProcessingPipeline.java`
  - explicit multi-stage processing lifecycle with status transitions
- `src/main/java/com/policymind/document/service/DocumentProcessingWorker.java`
  - first step toward background job separation
- `src/main/java/com/policymind/document/service/EmbeddingService.java`
  - provider abstraction for embeddings
- `src/main/java/com/policymind/document/service/OpenAiService.java`
  - structured JSON output contract and fallback behavior
- `src/main/java/com/policymind/document/service/VertexAiService.java`
  - alternate answer provider and embedding option
- `src/main/java/com/policymind/document/service/OutboundCallExecutor.java`
  - resilience policies for outbound model/service calls
- `src/main/java/com/policymind/document/model/Document.java`
  - persistent document-level workflow state
- `src/main/java/com/policymind/document/entity/DocumentChunk.java`
  - persisted chunk-level evidence with line metadata

### Current architectural limitations

- The async worker is still inside the same Spring Boot process.
- Document processing is a fixed pipeline, not a dynamic workflow.
- Q&A uses one retrieval pass plus one model answer pass.
- There is no planner, reviewer, verifier, or retry strategy at the reasoning level.
- There is no task graph or agent state machine.
- There is no separation between "simple question" and "complex investigative question."
- Embeddings are stored as JSON in PostgreSQL, which works now but is not ideal for high-scale semantic search.

## What "Agentic Framework" Should Mean Here

For this project, an agentic framework should not mean "a chatbot wrapper." It should mean a runtime that can:

- hold workflow state across steps
- choose tools dynamically
- branch based on intermediate results
- retry or escalate when confidence is low
- coordinate multiple specialized agents or roles
- produce auditable traces of reasoning decisions and tool usage

In PolicyMind, the best definition is:

"An agentic orchestration layer that sits above deterministic document services and uses tools, memory, retrieval, and guarded multi-step reasoning to produce better policy and document analysis outcomes."

## Where Agentic Architecture Fits Best in This Project

Agentic behavior is most valuable in the analysis layer, not the raw ingestion layer.

### Good candidates for agentic workflows

1. Complex document Q&A
- determine whether one retrieval round is enough
- reformulate the query if retrieval quality is weak
- compare answers from multiple providers
- run a verification pass before returning final output

2. Policy and contract risk review
- identify document type first
- choose a checklist or analysis template
- inspect clauses by category
- generate a structured risk report
- produce citations tied to chunk IDs and line ranges

3. Multi-document comparison
- compare two or more policies/contracts
- identify conflicts, gaps, or changed obligations
- synthesize delta summaries

4. Missing-information handling
- detect when uploaded text is low quality or incomplete
- recommend OCR, re-upload, or clarifying questions
- route the case to a fallback workflow

5. Adaptive retrieval
- use different retrieval strategies depending on the question
- expand search when confidence is low
- perform section-level follow-up retrieval before answering

6. Human-in-the-loop review
- pause high-risk outputs
- request approval for escalated findings
- mark outputs as draft, reviewed, or published

### Areas that should remain deterministic

1. File upload validation
2. MIME and extension checks
3. PDF text extraction
4. chunk generation
5. embedding generation
6. persistence
7. authentication and authorization
8. rate limiting and network resilience policies

These are system responsibilities, not reasoning responsibilities.

## Recommended Target Architecture

The best target design for this repository is a hybrid, event-driven, agent-assisted architecture.

### High-level runtime model

1. Frontend
- upload documents
- poll job state
- ask questions
- view analysis traces, confidence, citations, and review status

2. API Gateway / Core App
- keep Spring Boot as the main API and security boundary
- handle auth, uploads, job creation, status APIs, and result retrieval

3. Ingestion Worker Service
- deterministic processing only
- extract text
- chunk
- embed
- persist
- index in vector store

4. Agent Orchestrator Service
- runs agentic workflows
- selects tools
- manages graph/state transitions
- invokes retrieval, analysis, verification, and report assembly steps

5. Retrieval and Knowledge Layer
- vector search
- metadata filtering
- chunk and citation loading
- possibly policy templates/checklists

6. Persistence Layer
- PostgreSQL for system of record
- vector database or PostgreSQL + pgvector for semantic retrieval
- Redis for cache, short-lived workflow state, and queue support if needed

7. Event / Job Infrastructure
- queue for ingestion and analysis jobs
- dead-letter strategy for failures
- retries and visibility into job lifecycle

## Proposed Service Decomposition

### 1. Core API service

Keep the existing Spring Boot app, but narrow its responsibilities to:

- auth
- upload acceptance
- document metadata
- workflow/job creation
- status polling
- result retrieval
- review and approval APIs

This service remains the trusted system boundary.

### 2. Document ingestion service

Extract the current pipeline into a dedicated worker service.

Current code that maps naturally here:

- `DocumentProcessingPipeline`
- `PdfService`
- `ChunkService`
- `EmbeddingService`

Responsibilities:

- process upload jobs
- update document status
- persist extracted text and chunks
- create vector index entries
- emit completion/failure events

### 3. Agent orchestration service

This is the new heart of the re-architecture.

Responsibilities:

- decide which analysis workflow to run
- call retrieval tools
- perform reasoning in multiple steps
- validate output before publishing
- store agent trace, intermediate outputs, and confidence

This service should not replace your API service. It should be invoked by it.

### 4. Analysis/reporting service or module

Optional but useful if the product grows.

Responsibilities:

- generate policy review reports
- compare documents
- summarize risk findings
- support export formats

## Recommended Agent Roles

You do not need many agents at first. Start with a small set of specialized roles.

### 1. Planner agent

Purpose:
- understand the user request
- classify task type
- choose a workflow

Examples:
- simple Q&A
- deep risk review
- compare two contracts
- extract key obligations

### 2. Retrieval agent

Purpose:
- generate or rewrite search queries
- retrieve relevant chunks
- expand or narrow search when evidence is weak

### 3. Analyst agent

Purpose:
- reason over retrieved evidence
- create structured findings
- cite chunk IDs and line ranges

### 4. Verifier agent

Purpose:
- check if findings are actually supported by the retrieved evidence
- reject unsupported claims
- lower confidence when citations are weak

### 5. Output formatter agent

Purpose:
- assemble UI-ready JSON
- generate summaries, risk tables, and recommended actions

In many implementations, roles 3 and 4 can initially be two graph nodes using the same underlying model with different prompts and acceptance rules.

## Recommended Agentic Workflows

### Workflow A: Complex question answering

1. Planner classifies the request.
2. Retrieval agent performs initial semantic search.
3. Analyst drafts an answer with citations.
4. Verifier checks evidence sufficiency.
5. If confidence is low, retrieval agent runs a second search pass.
6. Analyst revises.
7. Final structured answer is stored and returned.

This is better than the current one-pass Q&A flow because it can recover from weak first retrieval.

### Workflow B: Policy or contract risk review

1. Planner identifies document type.
2. Template/tool selector chooses the appropriate checklist.
3. Retrieval agent gathers chunks per risk category.
4. Analyst creates clause findings by category.
5. Verifier checks each finding against source lines.
6. Formatter produces a final report with:
- executive summary
- risk score
- key risks
- recommended actions
- supporting citations

### Workflow C: Low-quality document triage

1. Ingestion service extracts text.
2. Quality evaluator checks text density, OCR quality, and section completeness.
3. If low quality:
- mark as `NEEDS_ATTENTION`
- recommend OCR or re-upload
- do not run full analysis

This avoids wasting LLM calls on unusable input.

### Workflow D: Multi-provider adjudication

1. Analyst runs with OpenAI.
2. Analyst runs with Vertex for the same task.
3. Verifier compares outputs.
4. If materially different:
- flag disagreement
- lower confidence
- optionally request human review

This is valuable in high-risk policy analysis scenarios.

## How to Map This onto the Existing Codebase

### Current to target mapping

Current: `DocumentController`
- Keep as API layer.
- Add endpoints for analysis jobs, traces, and review state.

Current: `DocumentService`
- Split into smaller orchestration services.
- Keep upload/job APIs in Spring Boot.
- Move complex reasoning orchestration out of this class.

Current: `DocumentProcessingWorker`
- Replace with queue-backed workers.
- Do not rely on in-process `@Async` for production-scale workflows.

Current: `DocumentProcessingPipeline`
- Keep the deterministic stages.
- Extract into its own worker/module/service.

Current: `EmbeddingService`
- Keep as a tool callable by ingestion and agent workflows.
- Add support for vector indexing abstraction.

Current: `OpenAiService` and `VertexAiService`
- convert into tool adapters for agent nodes
- preserve structured response contracts
- add usage metadata, latency, and token accounting

Current: `Document` table
- extend to store workflow type, analysis status, confidence, and review state

Current: `DocumentChunk` table
- keep citations and line ranges
- consider moving embeddings to a proper vector store or pgvector

## Suggested Data Model Additions

### Existing entities to keep

- `documents`
- `document_chunk`
- `users`

### New tables recommended

1. `analysis_job`
- job id
- document id
- job type
- status
- created at
- started at
- completed at
- failure reason
- requested by

2. `agent_run`
- run id
- analysis job id
- workflow name
- model/provider used
- status
- confidence
- total latency

3. `agent_step`
- run id
- step name
- input summary
- tool used
- output summary
- status
- started at
- completed at

4. `analysis_result`
- document id
- job id
- result JSON
- summary
- risk score
- confidence
- final citation set

5. `review_decision`
- result id
- reviewer
- action
- notes
- timestamp

This makes the agentic system observable and auditable.

## Recommended Framework Direction

Because the current backend is Java/Spring, there are two realistic paths.

### Option 1. Keep everything in Java

Use a Java-friendly orchestration approach with Spring-centered services and explicit workflow state machines.

Advantages:

- stays in one language ecosystem
- easier operational alignment with the current backend
- fewer cross-service language concerns

Disadvantages:

- fewer mature agentic orchestration options than Python
- more custom workflow logic may need to be built

### Option 2. Hybrid architecture with Python agent service

Keep Spring Boot for API/security/business system ownership, and introduce a Python-based agent orchestration service.

Recommended if the team wants stronger agent tooling quickly.

Good fit:

- Spring Boot remains the product backbone
- Python agent service runs graph-based workflows
- API service communicates with it through queue/events or internal HTTP/gRPC

Advantages:

- faster access to mature agent frameworks
- easier experimentation with planner/reviewer/retrieval graphs
- better ecosystem for evaluation and agent tracing

Disadvantages:

- two-language operational model
- more deployment and observability work
- contract design between services becomes important

### My recommendation

For this repository, the best practical approach is:

- keep Spring Boot as the main API and ingestion owner
- add a separate agent orchestration service for analysis workflows

That gives you the most value with the least disruption.

## Which Framework Style Fits Best

The best framework style for PolicyMind is graph-based orchestration, not fully autonomous open-ended agents.

Why:

- policy/document analysis requires traceability
- workflows need controlled branching
- outputs need citations and verification
- production systems need predictable step boundaries

So the agentic runtime should behave like:

- state machine
- graph workflow
- tool-calling orchestrator

Not like:

- unconstrained chat agent with broad autonomy

## Concrete Tooling the Agent Layer Should Have

Each agent should work through explicit tools.

### Retrieval tools

- `search_document_chunks(documentId, query, topK, filters)`
- `load_chunk_details(chunkIds)`
- `load_document_metadata(documentId)`

### Analysis tools

- `run_llm_analysis(provider, prompt, schema)`
- `compare_provider_outputs(runA, runB)`
- `score_evidence_support(answer, citations)`

### Ingestion/quality tools

- `check_text_quality(documentId)`
- `detect_document_type(documentId)`
- `extract_sections(documentId)`

### Governance tools

- `store_agent_step(...)`
- `mark_job_status(...)`
- `request_human_review(...)`

These should be narrow and auditable. Avoid giving the agent raw unrestricted database access.

## Why This Project Benefits from Agentic Design

### Advantages

1. Better answer quality for complex questions
- the system can retrieve, evaluate, retry, and verify instead of answering in one shot

2. Better handling of ambiguous documents
- the planner can choose different workflows for policies, contracts, forms, or low-quality scans

3. Improved traceability
- each step can be logged as a workflow event with citations and confidence

4. Better product expansion
- once the orchestration layer exists, you can add:
  - clause extraction
  - policy comparison
  - compliance review
  - red-flag detection
  - approval workflows

5. Safer high-value outputs
- verifier/reviewer steps reduce unsupported answers

6. More resilient multi-model strategy
- OpenAI and Vertex can be used for fallback, adjudication, or task specialization

7. Cleaner architecture
- deterministic ingestion remains simple
- adaptive reasoning moves into a purpose-built layer

## Disadvantages and Risks

### 1. More system complexity

Agentic systems add:

- more services
- more states
- more job tracking
- more testing complexity

This is the biggest tradeoff.

### 2. Higher cost

Multi-step reasoning means:

- more model calls
- more retrieval passes
- more token consumption

Without controls, costs can rise quickly.

### 3. Harder debugging

A deterministic pipeline is easier to debug than a branching workflow with retries and validation loops.

### 4. Latency risk

More steps can mean slower responses unless:

- analysis is asynchronous
- steps are parallelized where safe
- simple requests use a lightweight workflow

### 5. Hallucination does not disappear

Agents can still be wrong. In some cases they can be confidently wrong in multiple steps. Verification, citations, and guardrails are still required.

### 6. Governance requirements increase

If this system is used for legal/policy-sensitive output, you need:

- clear confidence labeling
- human review paths
- audit trails
- explicit non-legal-advice positioning if applicable

### 7. Framework lock-in risk

If too much business logic is embedded directly in one orchestration framework, future changes become harder.

Mitigation:

- keep tools and domain services framework-neutral
- keep prompts/schemas versioned
- make the workflow layer call domain APIs, not replace them

## Where Agentic Design Should Not Be Overused

Do not make these tasks agentic unless there is a strong reason:

- upload validation
- text extraction
- chunking
- line-number mapping
- basic persistence
- JWT auth
- reCAPTCHA checks
- simple status polling

These should remain deterministic because:

- they need predictability
- they are easy to test directly
- they do not benefit from LLM reasoning

## Suggested Rollout Plan

### Phase 1. Strengthen current architecture

Goals:

- keep current functionality stable
- prepare for extraction

Actions:

- split `DocumentService` into narrower services
- add explicit job entities instead of relying only on document status
- add vector-store abstraction
- capture richer metrics for retrieval and model calls

### Phase 2. Externalize background processing

Goals:

- remove in-process dependency on `@Async`

Actions:

- introduce queue-backed ingestion jobs
- run processing in dedicated workers
- add retries, dead-letter handling, and job observability

### Phase 3. Introduce agent orchestration for one workflow

Best first workflow:

- complex question answering over processed documents

Why:

- highest immediate product value
- easiest to compare with current one-pass RAG flow
- lower risk than full autonomous document review

### Phase 4. Add verifier and confidence gating

Actions:

- require citations for each major finding
- add evidence-support scoring
- route weak answers to low-confidence output or human review

### Phase 5. Expand into full document review workflows

Examples:

- contract red-flag review
- policy compliance review
- multi-document comparison
- executive summaries with evidence tables

## Recommended First Use Case

If the team wants the highest ROI starting point, the first agentic feature should be:

"Adaptive, citation-backed complex question answering over uploaded documents."

That means:

- users still upload and process PDFs the same way
- simple questions can still use the current fast path
- complex questions use planner -> retrieval -> analyst -> verifier
- the UI shows confidence, citations, and possibly model/provider source

This upgrades the existing Q&A flow without forcing a full rewrite.

## Suggested API Evolution

### Existing endpoints to keep

- `POST /upload`
- `GET /documents/{id}`
- `POST /{id}/ask`

### New endpoints to add

- `POST /documents/{id}/analysis-jobs`
- `GET /analysis-jobs/{jobId}`
- `GET /analysis-jobs/{jobId}/trace`
- `GET /analysis-jobs/{jobId}/result`
- `POST /analysis-jobs/{jobId}/review`

This gives the frontend visibility into long-running analysis work.

## Observability Requirements

Agentic systems are not production-safe without strong observability.

You should capture:

- job status changes
- model provider used
- prompt/template version
- retrieved chunk IDs
- evidence line ranges
- token usage
- latency per step
- fallback path taken
- confidence score
- human review actions

The project already has good service-level logging. Extend that into workflow-level tracing.

## Security and Governance Notes

Because this product handles uploaded documents and policy-style analysis:

- keep auth and authorization in the Spring Boot boundary
- do not let the agent access arbitrary data outside scoped tools
- redact secrets and sensitive fields from traces
- store only necessary workflow inputs/outputs
- define retention rules for uploaded content and analysis traces

If the product is used in regulated settings, agent trace design becomes a first-class requirement.

## Final Recommendation

PolicyMind should be re-architected as a hybrid platform:

- deterministic ingestion and storage services
- queue-backed worker execution
- a graph-based agent orchestration layer for complex analysis
- strong citation, confidence, and verification controls

The most important architectural principle is:

"Use agents for adaptive reasoning, not for basic system operations."

That keeps the system credible, testable, and scalable.

## Short Recommendation You Can Reuse

PolicyMind is already partway toward an agentic architecture because it has explicit ingestion stages, retrieval-backed AI answering, and an async processing boundary. The best next step is not to replace the current system with a fully autonomous agent, but to introduce a controlled agent orchestration layer above the existing Spring Boot services. In that model, ingestion, chunking, embeddings, persistence, auth, and resilience stay deterministic, while complex Q&A, policy review, validation, and multi-step analysis become graph-based agent workflows. This approach improves answer quality, traceability, and product flexibility, but it also adds cost, latency, and architectural complexity, so rollout should be staged and focused first on adaptive citation-backed document Q&A.
