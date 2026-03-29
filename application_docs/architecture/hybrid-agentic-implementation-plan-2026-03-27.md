# Hybrid Agentic Implementation Plan

Date: 2026-03-27
Project: PolicyMind Document Service
Audience: Engineering, product, architecture, delivery planning
Related: `application_docs/architecture/agentic-framework-rearchitecture-guide-2026-03-26.md`

## Executive Summary

This document translates the earlier agentic architecture recommendation into a concrete implementation plan for the current PolicyMind repository.

The recommended target is a hybrid platform with three capability areas:

1. Ingest
2. Analyze
3. Compose

That means the system should evolve from:

- PDF upload and processing
- chunking and embeddings
- retrieval-backed question answering

into a platform that can also:

- review and score policy documents
- compare policies and revisions
- revise an existing policy
- generate a new policy from templates and rules
- route high-risk outputs through verification and human review

The most important principle is:

Keep ingestion and storage deterministic.
Use agentic orchestration only for adaptive reasoning workflows.

## Current Baseline in This Repo

The current repository already has strong building blocks:

- Spring Boot API and security boundary
- PostgreSQL persistence
- Redis presence
- deterministic ingestion pipeline
- provider abstraction for OpenAI and Vertex
- pgvector-backed retrieval foundation
- async/background processing concept

Key current areas:

- `src/main/java/com/policymind/document/controller`
- `src/main/java/com/policymind/document/service`
- `src/main/java/com/policymind/document/repository`
- `src/main/java/com/policymind/document/model`
- `src/main/java/com/policymind/document/entity`
- `src/main/resources/application.yml`
- `docker-compose.yml`
- `docker-compose.aws.yml`

Current strengths:

- document ingest flow already exists
- document and chunk persistence already exists
- question answering already exists
- pgvector is now integrated
- model providers are already abstracted enough to reuse in an agent layer

Current gaps for the future platform:

- no workflow/job model beyond document status
- no analysis job tracking
- no policy draft/version model
- no clause library or template layer
- no policy revision workflow
- no policy generation workflow
- no agent runtime state model
- no reviewer/approver lifecycle
- no trace model for generated outputs

## Recommended Target Product Model

The future product should be designed around three domains.

### 1. Ingest

Responsibilities:

- upload PDF or source documents
- extract text
- chunk content
- embed and index content
- persist metadata and line-level references
- assess document quality

This remains deterministic.

### 2. Analyze

Responsibilities:

- question answering over uploaded documents
- policy review and risk scoring
- compare two or more policies
- detect missing clauses or problematic language
- produce citation-backed structured findings

This is where the first agentic workflow should be introduced.

### 3. Compose

Responsibilities:

- revise an existing policy
- propose clause updates
- generate a new policy from templates and business rules
- create a draft with rationale and citations
- run verifier and review stages before publish

This is the long-term direction and should be designed now, even if built later.

## Recommended Architecture for This Repo

The recommended architecture is:

### Keep in Spring Boot

- auth and security
- upload endpoints
- document metadata and status APIs
- ingestion job creation
- deterministic processing pipeline
- persistence ownership
- review and approval APIs
- policy workspace APIs

### Add a Separate Agent Service

Recommended responsibilities:

- planner workflow
- retrieval workflow
- analyst workflow
- verifier workflow
- drafting workflow
- revision workflow
- result trace persistence through service contracts

This should be a separate service rather than embedding all agent orchestration inside the current `DocumentService` class.

## Recommended Service Decomposition

### Service A: Core API and Domain Service

Keep the current repository as the main system of record and trusted API boundary.

Responsibilities:

- authentication and authorization
- document upload
- document status
- analysis job creation
- policy workspace CRUD
- review and approval actions
- results retrieval

Suggested package evolution inside current repo:

- `controller`
  - keep controllers thin
- `service`
  - split by domain instead of one large orchestration class
- `repository`
  - add workflow, draft, and review repositories
- `model` and `entity`
  - expand domain model

### Service B: Ingestion Worker

Can start as a module inside the current app, but should be designed for separation.

Responsibilities:

- process upload jobs
- extract text
- chunk text
- write chunk and vector records
- compute quality score
- emit completion/failure state

Likely code moved or reused from:

- `DocumentProcessingPipeline`
- `DocumentProcessingWorker`
- `PdfService`
- `ChunkService`
- `EmbeddingService`

### Service C: Agent Orchestrator

New service recommended.

Responsibilities:

- classify task type
- choose workflow
- perform adaptive retrieval
- run reasoning nodes
- run verification nodes
- build final result payloads
- support revision/generation workflows later

### Service D: Policy Composition Module

This may begin in the API service, then move out if needed.

Responsibilities:

- create draft from template
- revise existing policy version
- compute clause diffs
- produce redline-style suggestions
- store rationale and basis references

## Suggested Package Refactor in the Current Repo

This repo should evolve away from a controller + large service pattern into domain slices.

Recommended package direction:

- `com.policymind.document.analysis`
  - analysis job services
  - analysis result mappers
  - citations
  - confidence policies
- `com.policymind.document.ingestion`
  - upload processing
  - extraction
  - chunking
  - quality scoring
- `com.policymind.document.policy`
  - policy workspace
  - drafts
  - versions
  - clauses
  - templates
- `com.policymind.document.review`
  - approval flow
  - reviewer actions
  - audit state
- `com.policymind.document.ai`
  - provider adapters
  - prompt/schema contracts
  - model usage tracking
- `com.policymind.document.retrieval`
  - vector retrieval
  - metadata filters
  - citation assembly
- `com.policymind.document.jobs`
  - workflow state
  - queue/job orchestration

This can be phased in without a single massive refactor.

## Recommended New Domain Model

### Existing tables to keep

- `documents`
- `document_chunk`
- `users`

### New tables to add first

#### `analysis_job`

Purpose:
- explicit trackable workflow execution for analysis requests

Suggested fields:

- `id`
- `document_id`
- `job_type`
- `status`
- `requested_by`
- `created_at`
- `started_at`
- `completed_at`
- `failure_reason`
- `priority`
- `answer_provider`
- `embedding_provider`

Suggested `job_type` values:

- `QUESTION_ANSWER`
- `POLICY_REVIEW`
- `DOCUMENT_COMPARE`
- `POLICY_REVISE`
- `POLICY_GENERATE`

#### `analysis_result`

Purpose:
- persist structured output from analysis jobs

Suggested fields:

- `id`
- `analysis_job_id`
- `document_id`
- `summary`
- `confidence`
- `risk_score`
- `result_json`
- `created_at`
- `finalized_at`

#### `agent_run`

Purpose:
- top-level execution record for a graph/workflow run

Suggested fields:

- `id`
- `analysis_job_id`
- `workflow_name`
- `provider_summary`
- `status`
- `confidence`
- `latency_ms`
- `created_at`
- `completed_at`

#### `agent_step`

Purpose:
- auditable step-by-step trace

Suggested fields:

- `id`
- `agent_run_id`
- `step_name`
- `step_type`
- `status`
- `input_summary`
- `output_summary`
- `tool_name`
- `model_name`
- `latency_ms`
- `created_at`
- `completed_at`

### New policy composition tables to design now

These can be introduced in Phase 3 or 4.

#### `policy`

Purpose:
- logical policy record independent of individual versions

Suggested fields:

- `id`
- `name`
- `policy_type`
- `owner_user_id`
- `status`
- `created_at`
- `updated_at`

#### `policy_version`

Purpose:
- immutable version snapshots

Suggested fields:

- `id`
- `policy_id`
- `version_number`
- `source_document_id`
- `status`
- `created_by`
- `created_at`
- `published_at`

Suggested `status` values:

- `DRAFT`
- `UNDER_REVIEW`
- `APPROVED`
- `PUBLISHED`
- `REJECTED`

#### `policy_clause`

Purpose:
- clause-level structure for generation and revision

Suggested fields:

- `id`
- `policy_version_id`
- `clause_key`
- `title`
- `body`
- `sort_order`
- `source_type`
- `source_reference`

#### `clause_suggestion`

Purpose:
- AI-proposed add/edit/remove changes

Suggested fields:

- `id`
- `policy_version_id`
- `clause_id`
- `suggestion_type`
- `proposed_text`
- `rationale`
- `basis_json`
- `status`
- `created_at`

#### `policy_template`

Purpose:
- template-based policy generation

Suggested fields:

- `id`
- `template_name`
- `policy_type`
- `version`
- `content_json`
- `active`
- `created_at`

#### `review_decision`

Purpose:
- human-in-the-loop governance

Suggested fields:

- `id`
- `target_type`
- `target_id`
- `reviewer_user_id`
- `decision`
- `notes`
- `created_at`

## Recommended Workflow Types

### Workflow 1: Complex Q&A

This should be the first agentic workflow introduced.

Flow:

1. create `analysis_job`
2. planner classifies request complexity
3. retrieval node searches chunks
4. analyst node drafts answer with citations
5. verifier node checks evidence sufficiency
6. if weak, retrieval node runs second pass
7. final result saved in `analysis_result`

Why this first:

- directly extends an existing feature
- easiest to compare against the current one-pass implementation
- immediate user value
- lowest disruption to the rest of the app

### Workflow 2: Policy Review

Flow:

1. classify document or policy type
2. select checklist/template
3. gather evidence per category
4. build structured findings
5. verify citations
6. save review result
7. optionally route to human review

Suggested initial categories:

- governance
- security
- privacy
- retention
- access control
- incident response

### Workflow 3: Policy Revision

Flow:

1. select source policy version
2. collect user instructions and organization constraints
3. retrieve relevant clauses and comparable examples
4. propose clause-level edits
5. run verifier and consistency checks
6. save draft suggestions
7. reviewer accepts/rejects

### Workflow 4: Policy Generation from Scratch

Flow:

1. choose `policy_template`
2. gather org context and required rules
3. fill sections with generated draft content
4. run clause completeness check
5. run verifier
6. save as draft version
7. request review before publish

## What Should Stay Deterministic

Do not make these agentic:

- upload validation
- PDF parsing
- OCR readiness check
- chunking
- embeddings generation
- vector persistence
- auth and JWT
- reCAPTCHA
- raw database writes for core system state

These should stay testable, predictable, and low-cost.

## Agent Roles Recommended for PolicyMind

Start with these roles only.

### Planner

Purpose:
- route the request to the correct workflow

### Retrieval Agent

Purpose:
- build or refine search queries
- run semantic retrieval
- gather context evidence

### Analyst

Purpose:
- produce structured findings or draft output

### Verifier

Purpose:
- ensure findings are supported by evidence
- lower confidence or request retry when evidence is weak

### Drafting Agent

Purpose:
- write new clause content or revised draft language

### Formatter

Purpose:
- convert outputs into UI/API-safe JSON structures

This is enough for MVP. Do not start with a large swarm of agents.

## Recommended API Evolution

### Keep existing endpoints

- `POST /upload`
- `GET /documents/{id}`
- `POST /{id}/ask`

### Add analysis job endpoints

- `POST /documents/{id}/analysis-jobs`
- `GET /analysis-jobs/{jobId}`
- `GET /analysis-jobs/{jobId}/result`
- `GET /analysis-jobs/{jobId}/trace`

### Add policy workspace endpoints later

- `POST /policies`
- `GET /policies/{id}`
- `POST /policies/{id}/versions`
- `POST /policies/{id}/revise`
- `POST /policies/generate`
- `GET /policy-versions/{id}`
- `POST /policy-versions/{id}/review`
- `POST /policy-versions/{id}/publish`

## Suggested Implementation Phases

## Phase 0: Stabilize the Current Foundation

Status:
- mostly complete

Goals:

- pgvector integration
- SQL-based schema init
- focused smoke tests
- integration test scaffold

Done or in place:

- pgvector column and indexing
- SQL startup migration path
- focused service smoke tests
- Testcontainers-backed repository integration scaffold

Remaining small foundation tasks:

- fix local Docker/Testcontainers compatibility so integration test executes instead of skipping
- optionally move from `ddl-auto` toward explicit migrations later
- add better retrieval metrics

Effort estimate:
- `1-3 days`

## Phase 1: Introduce Explicit Analysis Jobs

Goals:

- remove analysis orchestration from the document entity alone
- make long-running AI operations observable and auditable

Changes:

- add `analysis_job` table
- add `analysis_result` table
- add analysis job service and repository
- add controller endpoints for job create/status/result
- refactor current `/ask` flow to support async mode for complex requests

Repo changes likely:

- new model/entity classes
- new repositories
- new controller for analysis jobs
- split `DocumentService` responsibilities

Suggested classes:

- `AnalysisJobService`
- `AnalysisResultService`
- `AnalysisJobController`
- `AnalysisWorkflowSelector`

Effort estimate:
- `1-2 weeks`

## Phase 2: Introduce First Agentic Workflow for Complex Q&A

Goals:

- improve answer quality without rewriting the entire platform

Changes:

- add workflow selector
- add planner/retrieval/analyst/verifier pipeline
- keep simple questions on cheap deterministic path
- store trace records in `agent_run` and `agent_step`

Architecture recommendation:

- begin with orchestrator module inside current repo if needed
- keep it behind an internal service boundary so it can be moved out later

Suggested classes or service contracts:

- `AgentOrchestrationClient`
- `QuestionAnswerWorkflow`
- `RetrievalTool`
- `VerificationService`
- `AgentTraceService`

Effort estimate:
- `2-4 weeks`

## Phase 3: Policy Review Workflow

Goals:

- turn the platform from generic document Q&A into policy intelligence

Changes:

- add document type detection
- add review templates/checklists
- add structured review categories
- add citation-backed findings and risk scoring
- add reviewer actions for high-risk outputs

Suggested domain additions:

- policy review template storage
- review category enums
- review decision table

Effort estimate:
- `2-4 weeks`

## Phase 4: Policy Revision Workspace

Goals:

- support revising existing policy documents

Changes:

- add `policy`, `policy_version`, `policy_clause`, `clause_suggestion`
- create draft workspace APIs
- add revision workflow
- save rationale and basis references
- add accept/reject flow for suggestions

This is where the system becomes more than an analyzer.

Effort estimate:
- `3-6 weeks`

## Phase 5: Policy Generation from Scratch

Goals:

- generate new policy drafts from templates, constraints, and rules

Changes:

- add template management
- add completeness and consistency checks
- support first-class draft generation job
- connect generation output to review and publish lifecycle

Effort estimate:
- `3-6 weeks`

## Overall Effort View

Approximate realistic range for this repo:

- internal prototype: `2-4 weeks`
- usable MVP with review and revision direction: `6-10 weeks`
- stronger production platform: `3-6 months`

This aligns with earlier effort guidance.

## Recommended Team Shape

### Smallest viable team

- 1 strong backend/full-stack engineer
- 1 AI/backend engineer part-time or full-time

### Better MVP team

- 1 backend/domain engineer
- 1 AI/platform engineer
- 1 frontend engineer part-time

### Production-ready path

- 2-4 engineers
- product/domain stakeholder input
- policy/legal reviewer input if outputs affect governance decisions

## Cost Guidance for the Next Phases

### Low additional cost work

- job tables
- API refactor
- policy workspace schema
- review workflow
- pgvector improvements

### Moderate cost work

- multi-step agentic Q&A
- verification pass
- storing traces and usage metrics

### Higher cost work

- policy generation from scratch
- multi-provider adjudication
- repeated draft/verify/revise loops

Cost-control recommendation:

- simple queries use one-pass retrieval path
- complex workflows use agent path only when needed
- generation and revision jobs should be async and explicitly created

## Major Risks and How to Control Them

### Risk 1: over-agentic design

Problem:
- too much of the system becomes expensive and hard to debug

Control:
- keep ingestion deterministic
- keep simple Q&A cheap
- gate agent workflows by complexity

### Risk 2: weak governance for generated policy text

Problem:
- generated clauses may be plausible but unsupported

Control:
- require basis references
- add verifier step
- require human approval for publish

### Risk 3: premature microservice complexity

Problem:
- too many services too early

Control:
- start with logical boundaries first
- extract the agent service when orchestration becomes real and stable

### Risk 4: poor data model for drafting

Problem:
- revision and generation become messy if policies are only raw documents

Control:
- introduce policy/version/clause model before heavy compose features

## Concrete Recommendation for the Next 2 Sprints

### Sprint 1

- formalize `analysis_job`, `analysis_result`, `agent_run`, `agent_step`
- split current `DocumentService` responsibilities
- add async analysis job endpoints
- keep current Q&A behavior working

### Sprint 2

- add first complex Q&A workflow
- add retrieval retry and verifier step
- store traces and confidence
- expose result and trace APIs

That gives the codebase a clean bridge from today’s RAG service to the future policy platform.

## Final Recommendation

The best next move for this repo is not full policy generation yet. The best next move is to build the workflow and trace foundation that both policy review and policy drafting will need later.

In practical terms, the order should be:

1. explicit analysis jobs
2. first agentic complex-Q&A workflow
3. policy review workflow
4. policy revision workspace
5. policy generation from templates and rules

This gives PolicyMind a controlled path from document service to policy intelligence platform without forcing an unstable big-bang rewrite.
