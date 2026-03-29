# Network Resilience Implementation Guide

Date: 2026-03-26
Project: PolicyMind Document Service
Audience: Senior developer / design review / implementation walkthrough

## Executive Summary

This service makes outbound network calls to third-party systems that are inherently unreliable under load and over time:

- OpenAI chat completions
- OpenAI embeddings
- Vertex AI chat generation
- Vertex AI embeddings
- Google reCAPTCHA verification

To harden those dependencies, we implemented the following resilience controls:

- timeouts
- retries
- circuit breakers
- bulkheads
- fallbacks where appropriate

The design choice was to implement these controls centrally for all outbound calls, and then apply them selectively at each integration boundary. This keeps the codebase consistent, tunable, and easier to reason about operationally.

## What Was Implemented

### 1. Shared resilience policy model

File:
- `src/main/java/com/policymind/document/config/NetworkResilienceProperties.java`

Purpose:
- Defines the resilience policy for outbound calls.
- Supports a default policy and per-integration overrides.
- Centralizes timeout, retry, circuit breaker, and bulkhead settings.

Why this was the right place:
- Resilience should be configurable, not hardcoded in each service.
- Different dependencies have different latency and concurrency characteristics.
- A property-backed model allows operational tuning without refactoring business logic.

Benefits:
- consistent resilience behavior across integrations
- safer production tuning
- clean separation between policy and call-site behavior
- easier SRE/operations handoff

### 2. Shared HTTP client factory

File:
- `src/main/java/com/policymind/document/config/NetworkClientFactory.java`

Purpose:
- Creates `RestTemplate` and `RestClient` instances with per-service connect and read timeouts.

Why this was the right place:
- Raw HTTP clients were previously being instantiated ad hoc inside services.
- Timeout configuration belongs at client construction time.
- Centralizing client creation prevents timeout drift between services.

Benefits:
- guarantees every outbound client has explicit timeouts
- removes duplicated client setup code
- ensures consistent low-level network behavior

### 3. Shared resilience execution wrapper

File:
- `src/main/java/com/policymind/document/service/OutboundCallExecutor.java`
- `src/main/java/com/policymind/document/service/OutboundCallTimeoutException.java`

Purpose:
- Wraps outbound calls in:
  - retry
  - circuit breaker
  - bulkhead
  - time limiter
- Uses per-service named policies such as `openai-chat`, `vertex-embedding`, and `recaptcha`.

Why this was the right place:
- The resilience concern is cross-cutting infrastructure, not business logic.
- Implementing it once avoids repeated and inconsistent error-handling code in every service.
- Named policies allow each external integration to be isolated and tuned independently.

Benefits:
- one implementation path for resilience behavior
- easier observability and future enhancement
- lower maintenance cost than repeating the same logic per service
- fewer bugs caused by inconsistent retry or timeout handling

## Where Each Pattern Was Applied

## Timeouts

### Where

Configured in:
- `src/main/java/com/policymind/document/config/NetworkClientFactory.java`
- `src/main/resources/application.yml`

Applied to:
- OpenAI chat
- OpenAI embeddings
- Vertex chat
- Vertex embeddings
- reCAPTCHA

### How

Two timeout layers were implemented:

1. Connect/read timeout at the HTTP client level
- prevents socket connect or response-read hangs

2. Call timeout via Resilience4j `TimeLimiter`
- caps the total time the application is willing to wait for the call

### Why these were suitable places

- HTTP-level timeout belongs where the client is created.
- End-to-end call timeout belongs where resilience policy is enforced.
- Using both protects against both slow networks and long-running upstream behavior.

### Benefits

- prevents thread starvation from hanging requests
- improves predictability under failure
- keeps async document processing from stalling indefinitely
- reduces tail-latency amplification during incidents

## Retries

### Where

Configured and applied in:
- `src/main/java/com/policymind/document/service/OutboundCallExecutor.java`

Used for:
- OpenAI chat
- OpenAI embeddings
- Vertex chat
- Vertex embeddings
- reCAPTCHA

### How

Retries are triggered only for retryable conditions such as:
- 429 rate limiting
- server-side failures
- network I/O problems
- timeouts
- connection failures
- circuit/bulkhead temporary rejection scenarios

They are intentionally not used for:
- invalid input
- local validation failures
- non-retryable business errors

### Why this was the right place

- Retry rules should be consistent and infrastructure-driven.
- Keeping retry classification centralized avoids each service inventing its own retry semantics.
- This reduces the risk of retrying non-idempotent or non-retryable failures incorrectly.

### Benefits

- improves success rate for transient failures
- smooths out temporary upstream throttling or short outages
- reduces user-visible failures for recoverable incidents
- creates a standard retry policy across all integrations

## Circuit Breakers

### Where

Configured and applied in:
- `src/main/java/com/policymind/document/service/OutboundCallExecutor.java`
- `src/main/resources/application.yml`

### How

Each named downstream integration gets its own circuit breaker instance.

Examples:
- `openai-chat`
- `openai-embedding`
- `vertex-chat`
- `vertex-embedding`
- `recaptcha`

When a downstream dependency crosses a failure threshold, the circuit opens and calls fail fast until the half-open probe window is reached.

### Why this was the right place

- Circuit breaking is a downstream-service concern, not a business-method concern.
- Naming breakers by dependency boundary is the cleanest microservice design choice.
- A shared executor ensures that all call sites consistently honor breaker state.

### Benefits

- prevents repeated expensive calls to an unhealthy dependency
- reduces cascading failures
- lowers pressure on already failing third-party systems
- improves recovery by allowing controlled half-open probing

## Bulkheads

### Where

Configured and applied in:
- `src/main/java/com/policymind/document/service/OutboundCallExecutor.java`
- `src/main/resources/application.yml`

### How

Each downstream integration is assigned a concurrency budget.

Examples:
- OpenAI chat has a smaller concurrent limit than embeddings
- Embeddings are allowed more parallelism because document processing may fan out chunk calls
- reCAPTCHA is allowed a separate concurrency pool because it serves auth flows and should not be blocked by AI traffic

### Why this was the right place

- Bulkheads should isolate by dependency type and business criticality.
- The system makes different kinds of outbound traffic with very different profiles.
- AI calls and security calls should not contend for the same resilience budget.

### Benefits

- prevents one dependency from exhausting all worker capacity
- protects auth/security paths from document-processing spikes
- contains blast radius during upstream latency incidents
- improves system fairness under load

## Fallbacks

### OpenAI chat fallback

File:
- `src/main/java/com/policymind/document/service/OpenAiService.java`

How:
- On quota exhaustion, timeout, circuit-open, bulkhead rejection, or generic call failure, the service returns a structured fallback JSON payload.

Why fallback is appropriate here:
- This call returns advisory AI output.
- A degraded but well-structured response is better than a hard failure for many UX flows.
- The caller already expects JSON, so preserving shape reduces downstream breakage.

Benefits:
- graceful degradation
- stable API contract under failure
- improved user experience
- easier frontend handling

### Vertex chat fallback

File:
- `src/main/java/com/policymind/document/service/VertexAiService.java`

How:
- Same strategy as OpenAI chat: return structured fallback JSON when the call cannot be completed safely.

Why fallback is appropriate here:
- This is also an advisory/generative response path.
- The consumer benefits more from predictable degraded output than from an exception.

Benefits:
- resilience without breaking response consumers
- easier provider substitution between OpenAI and Vertex

### Why no fallback vector for embeddings

Files:
- `src/main/java/com/policymind/document/service/EmbeddingService.java`
- `src/main/java/com/policymind/document/service/VertexAiService.java`

Decision:
- No synthetic embedding fallback was added.

Why this was correct:
- Fake or zero vectors would silently poison semantic search and ranking quality.
- Returning a fabricated embedding would hide a serious data-quality failure.
- For embeddings, failing explicitly is safer than degrading silently.

Senior-level rationale:
- not every network call should have a fallback
- the right fallback strategy depends on whether degraded output is still truthful and operationally safe

### Why reCAPTCHA fails closed instead of falling back open

File:
- `src/main/java/com/policymind/document/service/RecaptchaService.java`

Decision:
- If reCAPTCHA is unavailable, verification fails and the request is rejected.

Why this was correct:
- reCAPTCHA protects authentication-related flows
- bypassing verification during dependency failure would create a security hole
- security controls should degrade safely, not permissively

Senior-level rationale:
- for security-sensitive integrations, availability must not override trust guarantees

## Why These Were the Suitable Integration Points

### OpenAiService

Why suitable:
- This is the actual boundary where chat requests leave the service.
- It already owned fallback shaping, so it was the right place to preserve graceful degradation behavior.
- It was the cleanest place to separate transport concerns from prompt/response shaping.

### VertexAiService

Why suitable:
- This class encapsulates both Vertex chat and embedding integrations.
- It is the correct dependency boundary for provider-specific resilience handling.
- It allows independent isolation of Vertex traffic from OpenAI traffic.

### EmbeddingService

Why suitable:
- It is the abstraction the document pipeline uses for embeddings.
- Protecting this layer ensures all embedding generation paths get resilience without touching pipeline code.
- It preserves the provider-selection abstraction while hardening the actual calls.

### RecaptchaService

Why suitable:
- It is the only place where verification semantics are known.
- The service can correctly distinguish transport failure from validation failure.
- This is where security-aware fallback policy belongs.

### Configuration layer

Why suitable:
- Timeouts and resilience thresholds are operational parameters.
- They belong in config because real production tuning depends on traffic patterns, latency, quotas, and downstream SLAs.

## Practical Benefits to Call Out in a Senior-Level Explanation

When explaining this as a senior developer, the strongest points are:

### 1. We moved resilience from ad hoc error handling to a policy-driven model

That matters because:
- resilience becomes consistent
- operations can tune behavior centrally
- future integrations can adopt the same pattern quickly

### 2. We isolated dependencies by name and traffic type

That matters because:
- OpenAI outages do not have to consume the same resilience budget as reCAPTCHA
- chat and embedding workloads can be tuned differently
- the system is better protected from noisy-neighbor effects

### 3. We used fallback only where degraded output is still honest and safe

That matters because:
- AI text can degrade gracefully
- security checks must fail closed
- embeddings must not silently produce junk data

### 4. We protected both latency and capacity

That matters because:
- timeouts address long waits
- retries address transient failures
- circuit breakers address unhealthy downstreams
- bulkheads protect local capacity and isolate failure domains

## Suggested Senior-Developer Summary

If you need a concise explanation in a meeting, use this:

"We hardened every outbound dependency boundary with explicit timeout, retry, circuit-breaker, and bulkhead controls, and we centralized those policies so they are tunable and consistent. We applied fallbacks only where degraded output remains safe and contract-compatible, such as AI text generation. We intentionally did not fake embedding results, and we intentionally fail closed for reCAPTCHA because correctness and security matter more than permissive availability in those paths."

## Verification Performed

Targeted test command executed successfully:

```powershell
mvn -q "-Dtest=OpenAiServiceTest,EmbeddingServiceTest,AuthServiceTest,VertexAiServiceTest" test
```

## Files to Review During Walkthrough

Core implementation:
- `pom.xml`
- `src/main/java/com/policymind/document/config/NetworkResilienceProperties.java`
- `src/main/java/com/policymind/document/config/NetworkClientFactory.java`
- `src/main/java/com/policymind/document/config/ResilienceConfig.java`
- `src/main/java/com/policymind/document/service/OutboundCallExecutor.java`
- `src/main/java/com/policymind/document/service/OutboundCallTimeoutException.java`
- `src/main/java/com/policymind/document/service/OpenAiService.java`
- `src/main/java/com/policymind/document/service/VertexAiService.java`
- `src/main/java/com/policymind/document/service/EmbeddingService.java`
- `src/main/java/com/policymind/document/service/RecaptchaService.java`
- `src/main/resources/application.yml`

Updated tests:
- `src/test/java/com/policymind/document/service/OpenAiServiceTest.java`
- `src/test/java/com/policymind/document/service/EmbeddingServiceTest.java`
