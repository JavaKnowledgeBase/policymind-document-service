# 100 Basic Java Interview Questions Explained Through This Codebase

Date: 2026-03-26
Project: PolicyMind Document Service
Audience: Interview preparation using real project examples

## How to Use This Document

This is not just a list of generic Java questions. Each question is answered in terms of how the concept appears in this codebase — with real class names, real method signatures, and real design decisions — so you can explain Java from actual implementation experience instead of textbook recall. Where a design choice has a trade-off (why a field is a `String` instead of a `List<Double>`, why a constructor is package-private, why an exception is unchecked), the answer explains the reasoning, not just the fact, since that is what separates a junior answer from a senior one in an interview.

Primary code areas referenced:
- `src/main/java/com/policymind/document/service/`
- `src/main/java/com/policymind/document/controller/`
- `src/main/java/com/policymind/document/security/`
- `src/main/java/com/policymind/document/entity/`
- `src/main/java/com/policymind/document/repository/`
- `src/main/java/com/policymind/document/config/`

## Section 1: Java Fundamentals

### 1. What is Java?
Java is a statically-typed, object-oriented, platform-independent language that compiles to bytecode and runs on the JVM. This project's entire backend — controllers, services, entities, repositories, and security filters — is Java compiled with Maven and executed as a Spring Boot application. Choosing Java here (rather than a dynamically-typed language) buys compile-time type safety for a service that moves structured data — documents, chunks, embeddings, JWT claims — through many layers, where a typo in a field name is caught by the compiler instead of surfacing as a production bug.

### 2. What is the JVM?
The JVM is the runtime engine that loads class files, verifies bytecode, and executes it (interpreting cold code, JIT-compiling hot paths to native instructions). In this project, `mvn package` produces a runnable JAR, and that JAR is executed by a JVM process — either directly on a developer machine or inside the `Dockerfile`'s container image — which is what actually keeps `DocumentService`, `EmbeddingService`, and the rest of the Spring context alive and serving requests.

### 3. What is JDK vs JRE vs JVM?
- **JVM** executes the compiled bytecode.
- **JRE** is the JVM plus the standard library needed to run compiled code.
- **JDK** is the JRE plus development tooling — `javac`, `jar`, debuggers.

This project uses the JDK during the Maven build (compiling `.java` source into `.class` bytecode) and only needs a JVM/JRE at execution time. The `Dockerfile` reflects this split directly: a build stage typically uses a full JDK image to compile, while the final runtime image can use a slimmer JRE-only base to keep the deployed container smaller.

### 4. What is bytecode?
Bytecode is the platform-neutral, compiled intermediate form of a Java class — instructions for a stack-based virtual machine, not native machine code. Maven's `compile` phase turns every `.java` file in `src/main/java/com/policymind/document/` into a corresponding `.class` file containing this bytecode, which is then packaged into the executable JAR that the JVM actually runs.

### 5. What is a class?
A class is a blueprint that defines the state (fields) and behavior (methods) that its instances will have. Concrete examples from this codebase: `DocumentService` (8 constructor-injected collaborators plus business logic for submitting documents and answering questions), `OpenAiService` (wraps OpenAI chat/embedding calls behind resilience), and `JwtAuthenticationFilter` (a servlet filter class that extends `OncePerRequestFilter`).

### 6. What is an object?
An object is a runtime instance of a class, with its own state occupying heap memory. Spring's IoC container creates and owns exactly one object (a singleton bean, by default) for each of `DocumentService`, `EmbeddingService`, and `OpenAiService` at application startup, wiring them together via constructor injection rather than the application code calling `new` directly.

### 7. What is a package?
A package is a namespace that groups related classes and enforces the compile-time boundary for package-private (default) access. This codebase organizes by architectural layer: `service`, `controller`, `security`, `repository`, `entity`, `exception`, `enums`, and `config`, so `com.policymind.document.service.DocumentService` and `com.policymind.document.security.JwtAuthenticationFilter` are unambiguous, collision-free names even though many other Java projects will also have a class called `DocumentService`.

### 8. What is a method?
A method defines a unit of behavior scoped to a class. Real signatures from this codebase: `DocumentService.submitDocument(MultipartFile file)` (kicks off async processing and returns immediately with status `"QUEUED"`), `DocumentService.askQuestion(Long documentId, String question, String embeddingProvider, String answerProvider)`, and `OpenAiService.askLLM(String context, String question)` (wraps the actual OpenAI chat call in the shared resilience executor).

### 9. What is a constructor?
A constructor initializes a new object's state, typically its `final` fields. This codebase uses **constructor injection** almost everywhere Spring manages a bean — for example `SecurityConfig`'s constructor takes five collaborators (`JwtAuthenticationFilter`, two OAuth2 handlers, a custom OAuth2 user service, and an `@Value`-injected CORS origins string) and assigns them all to `private final` fields, so the object is fully and validly constructed the moment it exists — there's no window where it's half-initialized.

### 10. Why is constructor injection useful?
It makes a class's dependencies part of its public contract (you can't construct a `DocumentService` without supplying all eight of its collaborators), it lets fields be `final` and therefore immutable after construction, and it makes unit testing trivial — you can call `new DocumentService(mockRepo, mockChunkRepo, ...)` directly with mocks, with no need for a Spring context or reflection-based field injection. `OpenAiService` even goes a step further: it exposes a **second**, package-private constructor purely for tests, so test code can inject a mock `RestTemplate` directly while the real `@Autowired` constructor builds the production `RestTemplate` via `NetworkClientFactory`.

### 11. What is a variable?
A variable is a named, typed storage location. Real examples from the service layer: `String question` and `Long documentId` as method parameters in `askQuestion`, `Map<DocumentChunk, Double> similarityScores` inside the in-memory ranking fallback, and `String stage` in `DocumentProcessingPipeline`, which is reassigned at each processing step purely so a failure can be logged with the exact stage it happened at.

### 12. What are primitive types in Java?
Primitives (`int`, `long`, `double`, `boolean`, etc.) are the eight built-in, non-object value types. In this codebase, `int` drives index-based loops (chunk-boundary scanning in `ChunkService`), and `double` is the natural type for cosine-similarity scores computed while ranking chunks against a question's embedding — using a primitive here avoids boxing overhead in a tight numeric loop.

### 13. What are wrapper classes?
Wrapper classes (`Integer`, `Long`, `Double`, `Boolean`, ...) are object forms of primitives, needed anywhere a primitive can't be used directly — generic collections, JPA entity fields, or anywhere `null` (meaning "unknown"/"not applicable") is a valid state. This codebase's entities use them explicitly: `DocumentChunk.startLine` and `endLine` are `Integer`, not `int`, because a chunk that hasn't had line numbers computed yet needs to be representable as `null` rather than defaulting misleadingly to `0`.

### 14. Why use wrapper classes instead of primitives sometimes?
Because wrappers can be `null`, participate in generics (`List<Double>`, `Map<DocumentChunk, Double>`), and are required by JPA/Hibernate for entity identifier fields. That's exactly why `entity.User.id` and `entity.Document`'s identifier are `Long` rather than `long` — a transient, unsaved entity has no ID yet, and `Long id = null` cleanly expresses "not yet persisted," whereas a primitive `long` would be forced to a fake value like `0`.

### 15. What is strong/static typing in Java?
Java requires every variable, parameter, and return type to be declared and checked at compile time. That's why method signatures like `EmbeddingService.generateEmbedding(String text, String providerOverride)` and `DocumentService.askQuestion(Long documentId, String question, String embeddingProvider, String answerProvider)` are self-documenting about what they accept and return — the compiler rejects a caller who passes an `int` where a `Long` is expected, catching a class of bug before the code ever runs.

### 16. What is casting?
Casting explicitly converts a value from one type to another (widening/narrowing for primitives, or `(Type) value` for objects). Manual casting is deliberately rare in this codebase: strongly-typed method signatures and Jackson's `ObjectMapper.readValue(json, new TypeReference<Map<String, Object>>() {})` (used in `DocumentService.parseStructuredAnswer`) do the type conversion safely at the JSON boundary instead of the code casting raw `Object`s downstream.

### 17. What is `final`?
`final` prevents reassignment of a variable, overriding of a method, or subclassing of a class. Nearly every constructor-injected field in this codebase — `DocumentService`'s eight collaborators, `SecurityConfig`'s five — is declared `private final`, guaranteeing that once a bean is constructed, its dependencies can never be swapped out later, which rules out an entire category of "who reassigned this field" bugs in a multi-threaded Spring application.

### 18. What is `static`?
`static` members belong to the class itself, shared across all instances, rather than to any one object. Examples: `DocumentService.TOP_K = 3` (a shared constant capping how many chunks are retrieved per question) and `ChunkService.CHUNK_SIZE = 800` / `MAX_CHUNK_LENGTH = 3000` (shared chunking limits). Logger instances (`private static final Logger logger = ...`) are also `static` because logging configuration is a class-level concern, not something that should vary per instance.

### 19. Why use `static final` constants?
`static final` centralizes a fixed, shared value under one name instead of scattering the literal ("magic number") throughout the code, which makes intent explicit and changes safe. `ChunkService`'s `CHUNK_SIZE = 800` is used in exactly one place — the fixed-size fallback chunker — but naming it means a future reader immediately understands "this chunk splitting falls back to 800-character windows" instead of having to reverse-engineer a bare `800` in a loop bound.

### 20. What is scope in Java?
Scope is the region of code where a declared variable is visible and usable. Local variables like `StringBuilder context` inside `DocumentService`'s context-assembly logic, or `String stage` inside `DocumentProcessingPipeline.processStoredDocument`, exist only for the duration of that method call — they cannot leak into other requests, which matters a lot in a Spring singleton bean handling concurrent HTTP requests, where accidentally using an instance field instead of a local variable for per-request state would cause race conditions.

## Section 2: OOP Concepts

### 21. What is encapsulation?
Encapsulation bundles data with the behavior that operates on it and restricts direct external access to that data. `User` and `DocumentChunk` keep their fields `private` and expose them only through getters/setters (or through Spring/JPA's controlled reflective access), so nothing outside the entity can put it into an invalid state by reaching directly into its fields.

### 22. How is encapsulation visible in this codebase?
Fields across entities and services are declared `private`; `User` additionally uses Lombok's `@Getter`/`@Setter` to generate the accessor boilerplate (interestingly, `User` also has some hand-written getters/setters layered on top of the Lombok-generated ones — a bit of redundancy worth noticing and cleaning up, but functionally the encapsulation boundary is the same either way). `DocumentChunk`, by contrast, has no Lombok annotations at all and hand-writes every getter/setter explicitly — both approaches respect the same private-field, public-accessor discipline.

### 23. What is abstraction?
Abstraction exposes a simplified interface while hiding implementation complexity behind it. `EmbeddingService` is the textbook example here: callers invoke `generateEmbedding(text, providerOverride)` without needing to know whether the request ultimately goes to OpenAI's embeddings endpoint or Google's Vertex AI `:predict` endpoint — that branching (`resolveProvider`, then either `vertexAiService.generateEmbedding(...)` or the OpenAI path through `outboundCallExecutor`) is entirely hidden inside the service.

### 24. What is inheritance?
Inheritance lets a subclass reuse and specialize a superclass's implementation. `JwtAuthenticationFilter extends OncePerRequestFilter`, inheriting Spring Security's guarantee that its filter logic runs exactly once per request, and only needs to override the one method (`doFilterInternal`) that defines its specific behavior — extracting and validating the JWT.

### 25. What is polymorphism?
Polymorphism means the same call site can trigger different behavior depending on the actual runtime type or configuration behind an abstraction. In this codebase, `EmbeddingService.generateEmbedding(...)` and the analogous LLM-calling logic can route to either `OpenAiService` or `VertexAiService` based on a runtime `providerOverride`/configured default — callers write against one method and don't change when the underlying provider changes.

### 26. What is method overriding?
Overriding replaces an inherited method's implementation in a subclass, using the identical signature, resolved at runtime. `JwtAuthenticationFilter` overrides `doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)` from `OncePerRequestFilter` — Spring Security's filter chain calls this method polymorphically without knowing or caring that it's specifically a JWT filter underneath.

### 27. What is method overloading?
Overloading defines multiple methods with the same name but different parameter lists, resolved at compile time. `DocumentService.askQuestion` is overloaded: the four-argument version `askQuestion(Long documentId, String question, String embeddingProvider, String answerProvider)` contains the real logic, while `askQuestion(Long documentId, String question)` is a two-argument convenience overload that simply delegates to the first with `null, null` for the provider overrides.

### 28. What is the difference between overloading and overriding?
Overloading is same-class, same-name, different-parameter-list, resolved statically at compile time based on the declared argument types. Overriding is subclass-vs-superclass, identical signature, resolved dynamically at runtime based on the object's actual type. This codebase has a clean example of each: `askQuestion`'s two overloads (overloading) versus `JwtAuthenticationFilter.doFilterInternal` (overriding).

### 29. What is an enum?
An enum defines a fixed, closed set of named constants. `Role` defines exactly `USER` and `ADMIN`, and `User.role` is mapped with `@Enumerated(EnumType.STRING)`, so the database column stores the readable string `"ADMIN"` rather than an unstable ordinal integer — a deliberate choice that keeps the data readable and safe to reorder the enum constants later without corrupting existing rows.

### 30. Why use an enum for roles?
An enum gives compile-time safety (`Role.ADMIN` can't be mistyped the way a bare string `"admin"` or `"Admin"` could) and a closed, self-documenting set of valid values, which is exactly the property you want for something as security-sensitive as authorization roles. `JwtAuthenticationFilter` extracts the role from the JWT claim and maps it straight into a `SimpleGrantedAuthority("ROLE_" + role)` for Spring Security — enforcing that only the two defined roles can ever appear in that claim keeps the authorization surface small and predictable.

### 31. What is a POJO?
A POJO ("Plain Old Java Object") is a simple object with fields, constructors, and accessor methods, unconstrained by any framework-specific base class or interface. This codebase's entities (`User`, `DocumentChunk`, `Document`) are POJO-style — annotated with JPA annotations for persistence, but otherwise ordinary Java classes you could instantiate and use with `new` outside of any Spring context.

### 32. What is composition?
Composition builds a class's behavior by holding references to other objects and delegating to them, rather than inheriting from a shared superclass. `DocumentService` is a strong example: it is composed of eight collaborators — `DocumentRepository`, `DocumentChunkRepository`, `EmbeddingService`, `OpenAiService`, `VertexAiService`, `DocumentProcessingWorker`, `DocumentProcessingPipeline`, and `TrustedPolicyReferenceService` — and implements its business logic entirely by delegating to them, with no inheritance hierarchy involved at all.

### 33. Why is composition preferred over inheritance in many places?
Composition keeps each collaborator independently testable and replaceable (you can mock `EmbeddingService` without dragging in `OpenAiService`'s internals) and avoids the fragile-base-class problem where a change to a shared superclass ripples unpredictably into every subclass. This entire codebase is built with composition plus constructor injection as the default — the only real inheritance relationship of note is `JwtAuthenticationFilter extends OncePerRequestFilter`, which is inheriting a framework lifecycle contract, not sharing custom business logic.

### 34. What is cohesion?
Cohesion measures how focused a class's responsibility is. `ChunkService` only handles splitting document text into `ClauseChunk`s (detecting section headings, clause boundaries, and falling back to fixed-size windows); `OutboundCallExecutor` only handles wrapping a call with retry/circuit-breaker/bulkhead/timeout resilience. Neither class reaches outside its one job, which is what makes each of them independently understandable and testable.

### 35. What is coupling?
Coupling measures how much one class depends on the internal details of another. Constructor injection against interfaces/service classes (rather than concrete construction with `new`) keeps coupling loose in this codebase — `DocumentService` depends on `EmbeddingService`'s public method signatures, not on whether the embedding actually comes from OpenAI or Vertex, so that implementation detail can change without touching `DocumentService` at all.

## Section 3: Access Modifiers and Structure

### 36. What are access modifiers in Java?
They control visibility: `private` (class-only), package-private/default (same package), `protected` (package plus subclasses), and `public` (everywhere). Nearly every field in this codebase's entities and services is `private`, with access mediated through constructors, accessor methods, or Spring's dependency injection machinery.

### 37. Why are fields usually `private` here?
Private fields prevent external code from mutating an object's internal state in ways that bypass its invariants — a `DocumentChunk`'s `embedding` string, for instance, should only ever be set through the code path that actually serialized a valid embedding vector into JSON, not assigned an arbitrary string from anywhere else in the codebase.

### 38. What is package-private access?
Package-private (no explicit modifier) makes a member visible only within its own package — broader than `private`, narrower than `public`. `OpenAiService`'s second constructor (the one that accepts a `RestTemplate` directly) is package-private, which is a deliberate, real example of this access level being used for a specific purpose rather than as a default.

### 39. Why are some constructors not `public`?
A package-private constructor lets test code in the same package instantiate a service with test doubles directly — `OpenAiService`'s test-only constructor accepts a pre-built `RestTemplate`, so a test can supply a mock and exercise `askLLM`'s resilience/parsing logic without making a real HTTP call, all without exposing "construct me with a raw `RestTemplate`" as part of the class's genuine public API that other production code might misuse.

### 40. What is a nested type or record in this codebase?
A nested type is declared inside another class, scoping it to where it's actually used. The clearest example is `ChunkService.ClauseChunk` — a `public record` with twelve components (`content, startLine, endLine, chunkKind, sectionTitle, clauseType, domain, policyType, jurisdiction, sourceName, riskTags, referenceClause`) that represents one chunk of extracted document text along with all the metadata `ChunkService` inferred about it (heading, clause type, jurisdiction, risk tags), before that data ever becomes a persisted `DocumentChunk` entity.

## Section 4: Strings, Arrays, and Basic Logic

### 41. Why is `String` used so heavily in backend code?
Nearly everything this service touches is fundamentally text: uploaded PDF content, JSON request/response bodies, JWTs, LLM prompts and responses, file names, and serialized embedding vectors. `DocumentChunk.content`, `DocumentChunk.embedding` (a JSON-serialized string, not a native list — see Q56), and the entire prompt-building logic in `OpenAiService` are all `String`-based.

### 42. How are strings manipulated in this codebase?
Examples include: `OpenAiService.executeChatCompletion` building the system/user prompt via plain string concatenation; `ChunkService` scanning text line-by-line with a `StringBuilder currentClause` accumulator to detect clause boundaries; and `DocumentService`'s `toLineRangeLabel` helper formatting a chunk's line numbers into a display label like `"Line 12"` or `"Lines 12-18"`.

### 43. What is `StringBuilder` and why use it?
`StringBuilder` is a mutable, non-thread-safe character sequence, far more efficient than repeated `String` concatenation because it avoids allocating a brand-new `String` object at every `+=`. `DocumentService` uses exactly this pattern to assemble the retrieval context sent to the LLM — appending each top-ranked chunk's line-range label, chunk ID, and content into one `StringBuilder` before the loop finishes, rather than concatenating `String`s across iterations.

### 44. What is substring logic?
`substring(...)` extracts a portion of a `String` by character index. `ChunkService.fallbackFixedChunks` uses exactly this: `normalized.substring(i, Math.min(normalized.length(), i + CHUNK_SIZE))` inside a loop that advances `i` by `CHUNK_SIZE` (800) each time, producing fixed-size text windows when the smarter clause-boundary detection in `extractClauseChunks` doesn't find any recognizable structure to split on.

### 45. How are loops used here?
Traditional `for` loops appear anywhere index-based, positional logic is clearer than a stream — `ChunkService.fallbackFixedChunks`'s `for (int i = 0; i < normalized.length(); i += CHUNK_SIZE)` chunk-splitting loop is the cleanest example, since streams don't naturally express "advance by a fixed step and slice a substring at each position."

### 46. Why not always use streams instead of loops?
Because index-based operations — fixed-size chunk splitting, line-number bookkeeping — are often clearer and sometimes faster expressed as an explicit loop than forced into a stream pipeline. This codebase uses both deliberately: streams for the "transform, filter, rank, and collect a bounded top-N" work in `DocumentService` (a natural fit for `sorted().limit().map().collect()`), and plain loops for the positional substring-splitting work in `ChunkService`.

### 47. What is conditional logic?
Conditional logic branches behavior with `if`/`else` based on runtime state. Real examples: `EmbeddingService.resolveProvider` branching on whether an explicit provider override was passed versus falling back to the configured default; `DocumentService.findTopChunks` trying the pgvector-backed similarity search first and falling back to in-memory cosine similarity if that call throws; and `ChunkService` detecting whether a line is a section heading, a bullet, or ordinary clause text.

### 48. Why validate inputs early?
Failing fast at the boundary keeps invalid state from propagating deep into the system where it's harder to diagnose. `DocumentService.submitDocument` validates the uploaded file (rejecting empty files or unsupported types) before any async processing, embedding generation, or database writes are attempted — catching a bad upload in milliseconds rather than after a wasted PDF-extraction and LLM call.

### 49. What is a null check?
A null check guards against an absent value before it's dereferenced, avoiding `NullPointerException`. This codebase performs them around things like optional line-number metadata on a chunk, request body fields, and LLM response content that might come back malformed — `DocumentService.parseStructuredAnswer` explicitly builds a fallback response map when the LLM's JSON can't be parsed, rather than letting a `null`/malformed value crash the request.

### 50. Why are null-safe patterns important in Java?
Because this backend integrates with several systems — a database, two different AI providers, and file uploads — each of which can legitimately return nothing, fail, or return unexpected shapes, and an unguarded `null` dereference anywhere in that chain becomes a 500 error for the end user. `DocumentRepository.findById(...)` returning `Optional<Document>` (see Q76) rather than a raw, possibly-null `Document` is this codebase's main structural defense against that class of bug.

## Section 5: Collections and Generics

### 51. What is the Java Collections Framework?
It's the standard library of interfaces (`List`, `Map`, `Set`) and implementations (`ArrayList`, `HashMap`, `TreeMap`) for storing and manipulating groups of objects. This codebase leans on it constantly: lists of chunks, maps of similarity scores, and generic response payloads all flow through `List`/`Map` types.

### 52. What is a `List`?
A `List` is an ordered, index-accessible collection that permits duplicates. Real examples: `List<ChunkService.ClauseChunk>` returned from `extractClauseChunks`, and `List<DocumentChunk> topChunks` — the ranked, `TOP_K`-limited result that `DocumentService` feeds into prompt-context assembly.

### 53. What is a `Map`?
A `Map` associates unique keys with values. `DocumentService.findTopChunksInMemory` builds a `Map<DocumentChunk, Double> similarityScores` (each chunk mapped to its cosine-similarity score against the question embedding) purely as an intermediate structure to rank and select the top chunks — it's discarded once the sorted, limited `List<DocumentChunk>` is produced.

### 54. Why use `Map<String, Object>` in service responses?
It gives a flexible, dynamically-shaped structure for JSON-style API responses without needing a dedicated DTO class for every payload shape — `DocumentService.parseStructuredAnswer`'s fallback response (`summary`, `answer`, `confidence`, `risk_score`, `key_risks`, `recommended_actions`, `raw`) is built exactly this way, since its shape needs to tolerate a partially-malformed LLM response rather than fail a strict deserialization.

### 55. What is `ArrayList`?
`ArrayList` is the array-backed, resizable `List` implementation — fast random access, cheap appends at the end, amortized O(1) `add`. It's the natural (and default) choice anywhere this codebase builds up an ordered collection of chunks or scores by appending, such as the `List<ClauseChunk>` accumulated while scanning document text.

### 56. What is `HashMap`?
`HashMap` stores unordered key-value pairs with average O(1) lookup, insert, and delete. Beyond the similarity-score map in `DocumentService`, the *concept* also shows up in how embeddings are represented at rest: `DocumentChunk.embedding` is actually a plain `String` column holding **JSON-serialized** vector data (via `EmbeddingService.serializeEmbedding`/`deserializeEmbedding`, backed by Jackson), not a native `List<Double>` field — JPA/Hibernate has no first-class mapping for a raw list of doubles to a relational column, so the service layer serializes to JSON going in and parses it back going out.

### 57. What are generics?
Generics parameterize a type over another type, giving compile-time type safety for collections and APIs — `List<ChunkService.ClauseChunk>`, `Map<DocumentChunk, Double>`, `Optional<Document>` — instead of raw, unchecked `List`/`Map` types that would force casts and risk `ClassCastException` at runtime.

### 58. Why are generics important in this codebase?
Because the backend passes around many collections of very specific domain types — chunks, scores, claims, embedding values — and generics let the compiler catch a mistake like accidentally trying to `.add()` a `String` into a `List<ChunkService.ClauseChunk>` at compile time rather than discovering it as a runtime `ClassCastException` deep in a request path.

### 59. What is `List.of(...)`?
`List.of(...)` creates a fixed-size, structurally immutable list (any mutation attempt throws `UnsupportedOperationException`). It's the right tool for small, fixed sets of values that should never change after creation — a natural fit for things like a fixed set of default authorities or fallback values that this codebase treats as constants.

### 60. Why use immutable collection factories sometimes?
They express "this data is fixed and must not be mutated" directly in the type, which is self-documenting and prevents an entire class of bug where some downstream code accidentally calls `.add()`/`.remove()` on what was meant to be constant data and silently corrupts shared state — especially important for anything held by a singleton Spring bean and potentially touched by concurrent requests.

## Section 6: Exception Handling

### 61. What is an exception?
An exception represents an abnormal condition encountered during execution — a failed HTTP call, a malformed JSON response, an unreadable PDF. This backend uses both framework-thrown exceptions (`HttpClientErrorException.TooManyRequests` from a rate-limited OpenAI call) and its own custom exception (`DocumentProcessingException`).

### 62. What is the difference between checked and unchecked exceptions?
Checked exceptions (subclasses of `Exception` but not `RuntimeException`) must be declared or caught; unchecked exceptions (subclasses of `RuntimeException`) don't require either. This codebase deliberately favors unchecked exceptions at the service layer — `DocumentProcessingException extends RuntimeException` — so that internal service methods aren't forced to declare `throws` clauses that would ripple through every caller up to the controller layer.

### 63. Why is `DocumentProcessingException` a `RuntimeException`?
Document-processing failures (a bad PDF, an embedding call that fails after retries, a database write failure) are application-level failures that should propagate cleanly up through several layers of the service stack — `ChunkService` → `DocumentProcessingPipeline` → `DocumentService` → the controller — without every intermediate method signature having to declare `throws DocumentProcessingException`. Making it unchecked keeps that call chain readable while still letting the controller layer catch it explicitly where it matters.

### 64. How is exception translation used here?
Controllers convert domain/service exceptions into HTTP responses with the right status code and a client-safe message. `DocumentController` catches `DocumentProcessingException` and returns a `400`-range response body describing the failure, rather than letting a raw stack trace or a generic `500` reach the caller.

### 65. Why wrap low-level exceptions with business exceptions?
Wrapping adds domain context that the original exception doesn't carry. `DocumentProcessingException`'s two constructors — `DocumentProcessingException(String message)` and `DocumentProcessingException(String message, Throwable cause)` — let `DocumentProcessingPipeline` throw a message like `"Failed to process document at stage 'extract PDF text': <root cause>"` while preserving the original exception as the `cause`, so the stack trace still shows exactly what failed underneath, but the top-level message immediately tells a developer (or the logs) which pipeline stage broke.

### 66. What is try-catch used for in this codebase?
It brackets operations that can fail in ways the caller needs to handle deliberately: PDF text extraction, outbound HTTP calls to OpenAI/Vertex, LLM response JSON parsing, and the pgvector column update (which is wrapped in its own try-catch and treated as best-effort — a failure there is logged as a warning rather than failing the whole document-processing pipeline).

### 67. Why log inside catch blocks?
Because operational visibility is essential in a backend with several external dependencies (OpenAI, Vertex, Postgres) that can fail independently — `DocumentProcessingPipeline` logs the exact `stage` a failure occurred at, with the document ID, before rethrowing as `DocumentProcessingException`, so an on-call engineer reading logs doesn't have to reconstruct which of five processing steps actually broke.

### 68. What is a custom exception good for?
It names a specific domain failure mode instead of forcing every catch site to interpret a generic `RuntimeException`. `DocumentProcessingException` immediately tells a reader "something went wrong in document processing specifically," which both the controller's `catch` clause and anyone reading a stack trace can act on with more precision than a bare `Exception`.

### 69. How are parsing failures handled?
Both `OpenAiService` and `VertexAiService` wrap their response parsing defensively, and `DocumentService.parseStructuredAnswer` explicitly catches JSON parsing failures from `ObjectMapper.readValue(...)` and falls back to constructing a partial response map (`summary`, `answer`, `confidence`, `risk_score`, `key_risks`, `recommended_actions`, plus the `raw` text) rather than propagating the parse exception and failing the whole request.

### 70. Why is defensive exception handling important for external integrations?
Because third-party APIs — OpenAI, Vertex AI — can return throttled (429), malformed, partial, or unexpectedly-shaped responses that are entirely outside this codebase's control. `OpenAiService.askLLM` has layered catch blocks for exactly this: `HttpClientErrorException.TooManyRequests` specifically, then `CallNotPermittedException | BulkheadFullException | OutboundCallTimeoutException` (the resilience-layer failure modes), then a generic `Exception` catch-all — each returning a different, clearly-labeled fallback JSON reason string instead of letting any one of these failure types crash the request.

## Section 7: Java 8+ Features

### 71. What is a lambda expression?
A lambda is a concise, inline implementation of a functional interface. This codebase uses them for exactly the kind of "pass a small piece of behavior into a method" pattern they're designed for — `OpenAiService.askLLM` passes `() -> executeChatCompletion(context, question)` as the `Supplier<T>` given to `outboundCallExecutor.execute("openai-chat", ...)`, and `JwtAuthenticationFilter` extracts a claim with `claims -> claims.get("role", String.class)`.

### 72. What is a functional interface?
A functional interface declares exactly one abstract method, making it a valid lambda target. `OpenAiService` even defines its own: `@FunctionalInterface private interface StructuredResponseNormalizer { String normalize(String apiResponse); }`, used so the shared `executeStructuredChat` helper can accept a different response-shaping lambda depending on whether it's being called for a question-answer flow or for `composePolicyDraft` — a genuinely good example of defining a custom functional interface rather than reaching for a generic `java.util.function` type when the parameter needs a self-documenting name.

### 73. How are lambdas used in this project?
Beyond the claims-extraction and resilience-supplier examples above, `OutboundCallExecutor.execute(String serviceName, Supplier<T> supplier)` is the central place lambdas flow through this codebase — every outbound call to OpenAI, Vertex, or the embedding endpoints is wrapped as a `Supplier` lambda and threaded through `TimeLimiter.decorateFutureSupplier` → `CircuitBreaker.decorateCallable` → `Bulkhead.decorateCallable` → `Retry.decorateCallable` before finally being invoked.

### 74. What is a method reference or stream pipeline?
A stream pipeline processes a collection declaratively through chained intermediate/terminal operations; a method reference (`Map.Entry::getKey`) is shorthand for a lambda that just calls an existing method. `DocumentService.findTopChunksInMemory` uses both together: `similarityScores.entrySet().stream().sorted(Map.Entry.<DocumentChunk, Double>comparingByValue().reversed()).limit(TOP_K).map(Map.Entry::getKey).toList()` — sort by score descending, keep only the top `TOP_K` (3), then extract just the chunk from each entry.

### 75. Why use streams in `DocumentService`?
Because "rank a collection by a computed score, keep the top N, extract just the field you need" is precisely the map-sort-limit-collect shape streams were designed for — writing this with a manual loop, a temporary sorted list, and an explicit truncation would be considerably more verbose and more error-prone (off-by-one truncation bugs) than the one-line stream pipeline actually used here.

### 76. What is `Optional` in Java?
`Optional<T>` is a container that explicitly models "a value may or may not be present," used mainly as a return type to force callers to handle absence rather than risk an unguarded `null`. `DocumentRepository`, via `JpaRepository<Document, Long>`, gives `findById(id)` a return type of `Optional<Document>`, and calling code resolves it with `.orElseThrow(...)` to turn a missing document into a clear, immediate exception rather than a `NullPointerException` three method calls later.

### 77. Why is `Optional` useful in repositories?
It makes "this record might not exist" an explicit, compiler-enforced part of the method's return type instead of an implicit possibility a caller has to remember to check. `.orElseThrow(...)` at the call site turns a missing `Document` into a clear, immediate, purposeful exception rather than a silent `null` that might not fail until several calls later, in a much less informative stack trace.

### 78. What is a record in Java?
A record is a compact, immutable data-carrier class — declaring its components generates the constructor, accessors, `equals`/`hashCode`, and `toString` automatically. This codebase's real example is `ChunkService.ClauseChunk`, a twelve-component `public record` (`content, startLine, endLine, chunkKind, sectionTitle, clauseType, domain, policyType, jurisdiction, sourceName, riskTags, referenceClause`) that carries a fully-analyzed chunk of document text plus all the metadata inferred about it, before it's converted into a persisted `DocumentChunk` entity.

### 79. Why is `ClauseChunk` a good use of records?
It's exactly the shape a record is built for: an immutable bundle of related values with no behavior beyond simple accessors, produced once by `ChunkService.extractClauseChunks` and then read by `DocumentProcessingPipeline` to build the corresponding entity. Twelve components would be a lot of boilerplate (constructor, twelve getters, `equals`/`hashCode`/`toString`) to hand-write or even generate with Lombok — a record gets all of that for free from a single-line declaration.

### 80. What is `var` and is it used here?
`var` triggers local type inference — the compiler determines the concrete type from the initializer, but the variable is still statically typed underneath (it isn't dynamic typing). This codebase mostly sticks with explicit types (`List<DocumentChunk> topChunks` rather than `var topChunks`) in service-layer business logic, favoring the readability of an explicit type over the brevity of `var`, especially for method signatures and fields where inference doesn't even apply.

## Section 8: Concurrency and Async Basics

### 81. What is a thread?
A thread is an independent unit of execution within a process. In this backend, the HTTP request thread and the background document-processing thread are distinct — `DocumentProcessingWorker.processDocumentAsync(...)`, annotated `@Async`, runs on a thread from Spring's async task executor, separate from the thread handling the original upload request.

### 82. What is asynchronous processing?
Asynchronous processing lets work continue on a separate thread without blocking the caller. `DocumentService.submitDocument` is the clearest example: it validates the upload, persists a `Document` row with status `"QUEUED"`, calls `documentProcessingWorker.processDocumentAsync(...)`, and returns immediately — the actual PDF extraction, chunking, and embedding generation happen afterward, off the request thread.

### 83. How is async enabled in this codebase?
`@EnableAsync` is declared once, on `DocumentServiceApplication` (the `@SpringBootApplication` class) — not on `DocumentProcessingWorker` itself, which is a detail worth getting right in an interview, since it's a common assumption that the annotation lives on the class doing the async work. `DocumentProcessingWorker.processDocumentAsync(Long documentId, String fileName, byte[] fileBytes)` is then simply annotated `@Async`, and Spring proxies the call onto its task executor.

### 84. Why use async processing for document upload?
PDF text extraction, clause-boundary chunking, and generating an embedding per chunk can take real wall-clock time — long enough that making an HTTP client wait for all of it inline would be a poor experience and would tie up a request thread. Returning immediately with status `"QUEUED"` (via `submitDocument`) keeps the API responsive, and the client can poll for status separately (see `DocumentService.getDocumentStatus`) while the real work happens in the background.

### 85. What is a potential concurrency concern in systems like this?
Every outbound call — to OpenAI, to Vertex — can be slow or hang, and if request threads or async worker threads pile up waiting on a slow downstream dependency, the whole application can run out of capacity even though nothing has technically "crashed." This is precisely why the project built `OutboundCallExecutor` — a dedicated component that wraps every outbound call with a timeout, a circuit breaker, and a bulkhead, rather than letting calls block indefinitely on shared thread pools.

### 86. What is a bulkhead in code terms?
A bulkhead caps the number of concurrent calls allowed to a specific dependency, so that dependency exhausting its own capacity can't cascade into starving every other part of the system. `OutboundCallExecutor.execute(serviceName, supplier)` applies a per-`serviceName` Resilience4j `Bulkhead` (via `Bulkhead.decorateCallable`) — so, for example, a burst of slow OpenAI calls is limited to its own configured concurrency ceiling and can't consume all the threads that Vertex or database calls would otherwise need.

### 87. What is a timeout in concurrency terms?
A timeout bounds how long a caller will wait for an operation before giving up. `OutboundCallExecutor` applies a Resilience4j `TimeLimiter` (`TimeLimiter.decorateFutureSupplier`) around every outbound call, backed by a hand-built `ExecutorService` (`Executors.newCachedThreadPool(...)` with a custom daemon `ThreadFactory`), so a hung call to OpenAI or Vertex is forced to fail after its configured limit rather than tying up a worker thread indefinitely.

### 88. Why is fail-fast behavior useful in backend services?
Because a system that fails quickly and predictably when a dependency is unhealthy can recover and degrade gracefully (return a fallback response, retry later, or reject the next request cleanly), whereas a system that lets threads pile up waiting indefinitely eventually runs out of capacity for *everything*, including requests that had nothing to do with the failing dependency. `OutboundCallExecutor`'s full stack — retry, circuit breaker, bulkhead, timeout, all chained together via Resilience4j — exists specifically to make this backend fail fast and predictably under load rather than degrade invisibly.

## Section 9: I/O, JSON, and Integration Basics

### 89. How does this codebase handle file I/O?
Uploaded PDFs arrive as a Spring `MultipartFile`; `DocumentService` reads its bytes via `file.getBytes()` and passes the raw `byte[]` down through `DocumentProcessingWorker`/`DocumentProcessingPipeline`, so the whole extraction-and-chunking pipeline operates on an in-memory byte array rather than a filesystem path — there's no temp file written to disk for this step.

### 90. How does this codebase process PDF content?
`PdfService` extracts raw text from the uploaded PDF bytes; `ChunkService.extractClauseChunks` then scans that text line-by-line to detect section headings and clause boundaries (falling back to fixed-size `CHUNK_SIZE` windows if no structure is detected), producing the list of `ClauseChunk`s that `DocumentProcessingPipeline` turns into embedded, persisted `DocumentChunk` rows.

### 91. What is JSON mapping in Java?
It's the conversion between Java objects and JSON text, handled here by Jackson's `ObjectMapper`. `DocumentService.parseStructuredAnswer` calls `objectMapper.readValue(llmResponseJson, new TypeReference<>() {})` to turn the LLM's structured JSON answer into a `Map<String, Object>`, and `EmbeddingService.serializeEmbedding`/`deserializeEmbedding` use `ObjectMapper` in the other direction, to move a `List<Double>` embedding vector into and out of a database `TEXT` column.

### 92. Why use `ObjectMapper` here?
Because this service both talks to JSON-speaking APIs (OpenAI, Vertex chat/embedding endpoints) and needs to persist structured data (embedding vectors) into a relational column that has no native array/vector type mapping — `ObjectMapper` is the single, well-tested tool that handles both directions, rather than the codebase hand-rolling JSON parsing or string formatting.

### 93. What is serialization?
Serialization converts in-memory data into a transportable/storable format. `EmbeddingService.serializeEmbedding(List<Double>)` is the concrete example — it turns a Java `List<Double>` into a JSON array string (via `objectMapper.writeValueAsString(...)`) before that value is written into `DocumentChunk.embedding`, a plain `TEXT` column.

### 94. What is deserialization?
Deserialization is the reverse: turning stored/received structured text back into Java objects. `EmbeddingService.deserializeEmbedding(String embeddingJson)` reads the JSON array back out of the database (via `objectMapper.readTree(...)`, iterating the array nodes into a `List<Double>`) whenever the in-memory cosine-similarity fallback path needs the actual vector values to compare against a question's embedding.

### 95. Why store embeddings as JSON strings here?
It's a pragmatic persistence strategy: `DocumentChunk.embedding` is declared as a plain `String` column (`@Column(columnDefinition = "TEXT")`), which needs no special database extension or custom JPA type converter to work. For actual vector similarity search, the project separately maintains a **pgvector** column, populated out-of-band via `DocumentChunkRepository.updateEmbeddingVector(...)` rather than through the JPA-mapped `embedding` field — so the JSON string is the portable, always-available representation, and the pgvector column is an optional performance path that `DocumentService.findTopChunks` tries first and falls back away from (to in-memory cosine similarity over the JSON-deserialized vectors) if it's unavailable.

## Section 10: Enterprise Java and Spring-Friendly Concepts

### 96. What is dependency injection?
Dependency injection means a class receives its collaborators from an external source (here, the Spring IoC container) instead of constructing them itself. Every `@Service`, `@Component`, and `@Configuration` class in this codebase — `DocumentService`, `OpenAiService`, `SecurityConfig`, `OutboundCallExecutor` — declares its dependencies as constructor parameters and lets Spring resolve and wire them at startup.

### 97. Why is dependency injection good for interviews to mention?
Because it directly demonstrates modularity, testability, and separation of concerns with a concrete example, not just the definition: `OpenAiService` can be unit-tested by constructing it with its package-private test constructor and a mocked `RestTemplate`, entirely without a running Spring context — that's the practical payoff of constructor-based DI, not just a textbook benefit.

### 98. What is an interface-based repository in Java enterprise code?
A repository interface expresses persistence operations declaratively, with Spring Data JPA generating the implementation at runtime — no hand-written SQL or DAO boilerplate needed for standard CRUD. `DocumentRepository extends JpaRepository<Document, Long>` is a literal one-line empty interface in this codebase, and it already provides `save`, `findById` (returning `Optional<Document>`), `findAll`, `delete`, and more, entirely generated.

### 99. What is the benefit of separating controller, service, and repository layers?
It isolates three distinct concerns — HTTP transport, business logic, and persistence — so each can change independently: swapping the database technology wouldn't require touching `DocumentController`, and changing how a document status response is shaped wouldn't require touching `DocumentRepository`. This codebase follows that layering consistently (`controller` → `service` → `repository`), plus a `security` layer that cross-cuts all of it via `JwtAuthenticationFilter`.

### 100. If asked "How do you show strong Java fundamentals from a real project?", what should you say?
You can say this codebase demonstrates core Java through OOP (constructor-injected composition over inheritance), encapsulation, a single meaningful inheritance relationship (`JwtAuthenticationFilter extends OncePerRequestFilter`), enums for closed value sets (`Role`), custom unchecked exceptions with cause-chaining (`DocumentProcessingException`), collections and generics used precisely (`Map<DocumentChunk, Double>` for ranking), streams for rank-and-limit logic, a well-chosen record (`ClauseChunk`) for an immutable data carrier, `@Async`-based background processing, Jackson-based JSON (de)serialization for both API payloads and embedding persistence, and layered resilience (retry/circuit-breaker/bulkhead/timeout via `OutboundCallExecutor`) around every external call. Then point to concrete files: `DocumentService`, `JwtAuthenticationFilter`, `Role`, `DocumentProcessingException`, `ChunkService`, `OutboundCallExecutor`.

## Strong Short Summary for Interviews

If you need a concise closing summary, use this:

"In this project I used core Java in practical backend scenarios: classes and objects for domain modeling, constructor-based dependency injection (including a package-private test constructor in `OpenAiService` for injecting mocks), a real inheritance relationship in the JWT filter, enums for authorization roles, a custom unchecked exception with cause-chaining for domain failures, precise generic collections for ranking retrieved chunks by similarity score, streams for the sort-limit-collect ranking logic, a twelve-field record for an immutable chunk data carrier, `@Async` background processing for document ingestion, and Jackson-based JSON handling for both API responses and embedding persistence. On top of that, every outbound AI call is wrapped in a dedicated resilience layer — retry, circuit breaker, bulkhead, and timeout — which is where a lot of the more advanced Java concurrency and design-pattern thinking in this codebase actually lives."

## Best Files to Review Before an Interview

- `src/main/java/com/policymind/document/service/DocumentService.java`
- `src/main/java/com/policymind/document/service/DocumentProcessingPipeline.java`
- `src/main/java/com/policymind/document/service/ChunkService.java`
- `src/main/java/com/policymind/document/service/OpenAiService.java`
- `src/main/java/com/policymind/document/service/EmbeddingService.java`
- `src/main/java/com/policymind/document/service/OutboundCallExecutor.java`
- `src/main/java/com/policymind/document/service/DocumentProcessingWorker.java`
- `src/main/java/com/policymind/document/security/JwtAuthenticationFilter.java`
- `src/main/java/com/policymind/document/security/SecurityConfig.java`
- `src/main/java/com/policymind/document/entity/User.java`
- `src/main/java/com/policymind/document/entity/DocumentChunk.java`
- `src/main/java/com/policymind/document/enums/Role.java`
- `src/main/java/com/policymind/document/exception/DocumentProcessingException.java`
