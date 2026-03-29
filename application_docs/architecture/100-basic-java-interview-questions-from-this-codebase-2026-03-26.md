# 100 Basic Java Interview Questions Explained Through This Codebase

Date: 2026-03-26
Project: PolicyMind Document Service
Audience: Interview preparation using real project examples

## How to Use This Document

This is not just a list of generic Java questions. Each question is answered in terms of how the concept appears in this codebase so you can explain Java from real implementation experience.

Primary code areas referenced:
- `src/main/java/com/policymind/document/service/`
- `src/main/java/com/policymind/document/controller/`
- `src/main/java/com/policymind/document/security/`
- `src/main/java/com/policymind/document/entity/`
- `src/main/java/com/policymind/document/repository/`
- `src/main/java/com/policymind/document/config/`

## Section 1: Java Fundamentals

### 1. What is Java?
Java is an object-oriented, class-based language used here to implement the backend service. The Spring Boot application, controllers, services, entities, and repositories are all written in Java.

### 2. What is the JVM?
The JVM runs compiled Java bytecode. This project is compiled by Maven and then executed as a Spring Boot JVM application, locally or inside Docker.

### 3. What is JDK vs JRE vs JVM?
- JVM runs the code
- JRE provides runtime libraries plus JVM
- JDK provides development tools like `javac`
This project uses the JDK during build and the JVM during execution.

### 4. What is bytecode?
Bytecode is the compiled intermediate form of Java classes. Maven compiles this codebase into bytecode before the Spring Boot app starts.

### 5. What is a class?
A class is a blueprint for objects. Examples in this repo are `DocumentService`, `OpenAiService`, and `JwtAuthenticationFilter`.

### 6. What is an object?
An object is a runtime instance of a class. For example, Spring creates and manages runtime objects for `DocumentService` and `AuthService`.

### 7. What is a package?
A package organizes related classes. This codebase uses packages like `service`, `controller`, `security`, `repository`, and `entity`.

### 8. What is a method?
A method defines behavior inside a class. Examples include `submitDocument(...)` in `DocumentService` and `askLLM(...)` in `OpenAiService`.

### 9. What is a constructor?
A constructor initializes objects. This codebase heavily uses constructor injection, for example in `DocumentService`, `SecurityConfig`, and `ChunkService`.

### 10. Why is constructor injection useful?
It makes dependencies explicit, improves testability, and avoids hidden mutable state. This is why most Spring beans here accept dependencies through constructors.

### 11. What is a variable?
A variable stores data. For example, `String question`, `Long documentId`, and `List<Double> embeddingVector` are variables used throughout services.

### 12. What are primitive types in Java?
Primitives include `int`, `double`, `boolean`, etc. In this codebase, `int` is used for chunk sizing and loop indexes, while `double` is used for similarity scoring.

### 13. What are wrapper classes?
Wrappers are object forms of primitives such as `Integer`, `Long`, and `Double`. JPA entities here use wrappers like `Long id`, `Integer startLine`, and `Double` inside embedding lists.

### 14. Why use wrapper classes instead of primitives sometimes?
Wrappers support `null`, generics, and frameworks like JPA. That is why entity IDs are `Long` rather than `long`.

### 15. What is type inference or strong typing in Java?
Java is strongly typed, so every variable has a declared type. That makes interfaces like `Map<String, Object>` and `List<DocumentChunk>` explicit and safer.

### 16. What is casting?
Casting converts one type to another. In this codebase, explicit conversion is limited because strong model typing and Jackson mapping reduce manual casting.

### 17. What is `final`?
`final` prevents reassignment for variables and inheritance/override for classes or methods. This project uses many `private final` fields in services to keep dependencies immutable after construction.

### 18. What is `static`?
`static` belongs to the class rather than an instance. Examples include `static final Logger logger` and constants like `CHUNK_SIZE` in `ChunkService`.

### 19. Why use `static final` constants?
It centralizes shared fixed values and avoids magic numbers. `ChunkService` uses a `CHUNK_SIZE` constant for chunk splitting.

### 20. What is scope in Java?
Scope defines where a variable is visible. Local variables inside methods such as `response`, `body`, or `questionEmbedding` exist only within those methods.

## Section 2: OOP Concepts

### 21. What is encapsulation?
Encapsulation means keeping data and behavior together and controlling access through methods. Entities like `User` and `DocumentChunk` expose fields through getters and setters.

### 22. How is encapsulation visible in this codebase?
Fields are generally `private`, while access is provided through methods or framework-managed binding. This keeps internal state controlled.

### 23. What is abstraction?
Abstraction hides implementation details behind a simpler interface. `EmbeddingService` abstracts whether embeddings come from OpenAI or Vertex.

### 24. What is inheritance?
Inheritance lets one class extend another. `JwtAuthenticationFilter` extends `OncePerRequestFilter`, inheriting filter lifecycle behavior.

### 25. What is polymorphism?
Polymorphism means code can behave differently through the same abstraction. In this codebase, the embedding path can switch between OpenAI and Vertex providers without changing caller code.

### 26. What is method overriding?
Overriding means redefining inherited behavior. `JwtAuthenticationFilter` overrides `doFilterInternal(...)` from `OncePerRequestFilter`.

### 27. What is method overloading?
Overloading means same method name, different parameter list. `DocumentService` has overloaded `askQuestion(...)` methods.

### 28. What is the difference between overloading and overriding?
Overloading happens in the same class with different signatures. Overriding changes inherited behavior in a subclass. This repo contains examples of both.

### 29. What is an enum?
An enum represents a fixed set of constants. `Role` defines `USER` and `ADMIN` for authorization-related behavior.

### 30. Why use an enum for roles?
It prevents typo-prone string literals and gives compile-time safety. That is why `User` stores `Role` rather than arbitrary text.

### 31. What is a POJO?
A POJO is a plain old Java object with fields and methods. Entities and DTO-like request models in this codebase are POJO-style classes.

### 32. What is composition?
Composition means building a class from other objects. `DocumentService` composes repositories and other services to implement document workflows.

### 33. Why is composition preferred over inheritance in many places?
It keeps responsibilities modular and avoids rigid class hierarchies. Most business logic in this repo is built with composition plus constructor injection.

### 34. What is cohesion?
Cohesion means a class has a focused responsibility. `RecaptchaService` only handles verification, and `ChunkService` only handles chunking-related logic.

### 35. What is coupling?
Coupling means how dependent classes are on each other. Constructor injection and clear service boundaries help keep coupling manageable here.

## Section 3: Access Modifiers and Structure

### 36. What are access modifiers in Java?
They control visibility: `private`, `protected`, package-private, and `public`. Most fields in this project are `private`.

### 37. Why are fields usually `private` here?
Private fields protect object state and force access through methods or controlled framework access.

### 38. What is package-private access?
It means no explicit modifier. It is useful for internal helpers. This codebase uses package-level constructors in some services to support testing.

### 39. Why are some constructors not `public`?
Package-visible constructors let tests instantiate services directly without exposing those constructors as part of the broad public API.

### 40. What is a nested type or record?
A nested type is defined inside another class. `DocumentProcessingPipeline` defines a private `record LineRange(...)` to represent chunk line metadata cleanly.

## Section 4: Strings, Arrays, and Basic Logic

### 41. Why is `String` used so heavily in backend code?
HTTP payloads, JSON, prompts, JWTs, filenames, and text extraction all operate on text. This backend processes large amounts of string data.

### 42. How are strings manipulated in this codebase?
Examples include building prompts in `OpenAiService` and `VertexAiService`, trimming inputs, substring chunking, and line-range labeling.

### 43. What is `StringBuilder` and why use it?
`StringBuilder` is efficient for repeated string concatenation. `DocumentService` uses it to assemble the retrieval context from top document chunks.

### 44. What is substring logic?
Substring extracts part of a string. `ChunkService.chunkText(...)` uses `substring(...)` to split long text into fixed-size chunks.

### 45. How are loops used here?
Traditional `for` loops are used for chunking, vector parsing, similarity computation, and line counting.

### 46. Why not always use streams instead of loops?
Simple loops are sometimes clearer and faster for index-based operations like chunking, cosine similarity, or line-number calculation.

### 47. What is conditional logic?
Conditional logic uses `if` statements to branch behavior. Examples include provider selection, input validation, empty checks, and fallback selection.

### 48. Why validate inputs early?
Failing early simplifies downstream logic and avoids invalid state. `DocumentService` rejects empty files and unsupported file types early.

### 49. What is a null check?
A null check guards against absent values. This codebase uses null checks for request values, response content, and optional data like line numbers.

### 50. Why are null-safe patterns important in Java?
Because null references can cause runtime failures. This backend is full of integrations and persistence operations where null handling matters.

## Section 5: Collections and Generics

### 51. What is the Java Collections Framework?
It provides data structures like `List`, `Map`, and `Set`. This codebase uses collections everywhere for chunks, embeddings, responses, and authorities.

### 52. What is a `List`?
A `List` is an ordered collection. Examples include `List<String> chunks`, `List<Double> embeddings`, and `List<DocumentChunk> topChunks`.

### 53. What is a `Map`?
A `Map` stores key-value pairs. This project uses `Map<String, Object>` for JSON-style responses and prompt/request bodies.

### 54. Why use `Map<String, Object>` in service responses?
It is flexible for shaping dynamic JSON responses quickly, especially for API payloads and structured AI output.

### 55. What is `ArrayList`?
`ArrayList` is a resizable array-backed list. It is used in chunk generation and vector assembly because order matters and append operations are common.

### 56. What is `HashMap`?
`HashMap` stores unordered key-value pairs with fast lookup. It is used to build JSON-like structures and similarity score maps.

### 57. What are generics?
Generics let collections be type-safe, like `List<Double>` or `Map<String, Object>`. They reduce casting and improve compile-time checks.

### 58. Why are generics important in this codebase?
The backend works with many collections of domain-specific types. Generics make the code safer and easier to understand.

### 59. What is `List.of(...)`?
It creates an immutable list. This codebase uses it for fixed response values and simple authorities or fallback arrays.

### 60. Why use immutable collection factories sometimes?
They express intent clearly for fixed data and help avoid accidental modification.

## Section 6: Exception Handling

### 61. What is an exception?
An exception represents an error condition during runtime. This backend uses both framework exceptions and custom exceptions.

### 62. What is the difference between checked and unchecked exceptions?
Checked exceptions must be declared or caught. Unchecked exceptions extend `RuntimeException`. This codebase mostly uses unchecked exceptions for service-layer failures.

### 63. Why is `DocumentProcessingException` a `RuntimeException`?
Document processing failures are application-level failures that should propagate through the service stack without cluttering every method signature.

### 64. How is exception translation used here?
Controllers translate domain/service exceptions into HTTP responses. `DocumentController` catches `DocumentProcessingException` and returns a `400` response body.

### 65. Why wrap low-level exceptions with business exceptions?
It gives clearer domain context. For example, processing errors are rethrown as `DocumentProcessingException` with stage-aware messages.

### 66. What is try-catch used for in this codebase?
It is used around file reading, downstream API calls, parsing, and async processing to control behavior under failure.

### 67. Why log inside catch blocks?
Because operational visibility matters in backend systems. Services log failures before returning fallbacks or rethrowing exceptions.

### 68. What is a custom exception good for?
It makes intent clearer than generic runtime failures. `DocumentProcessingException` communicates a domain-specific processing problem.

### 69. How are parsing failures handled?
AI response parsing is wrapped defensively in `OpenAiService`, `VertexAiService`, and `DocumentService.parseStructuredAnswer(...)`.

### 70. Why is defensive exception handling important for external integrations?
Because third-party APIs can return malformed, partial, throttled, or unexpected responses.

## Section 7: Java 8+ Features

### 71. What is a lambda expression?
A lambda is a concise implementation of a functional interface. This codebase uses lambdas in places like resilience execution and claim extraction.

### 72. What is a functional interface?
A functional interface has one abstract method and can be implemented by a lambda. Spring and resilience libraries use them heavily here.

### 73. How are lambdas used in this project?
Examples include `claims -> claims.get("role", String.class)` and supplier lambdas passed to `OutboundCallExecutor.execute(...)`.

### 74. What is a method reference or stream pipeline?
A stream pipeline processes collections declaratively. `DocumentService` uses streams to sort similarity scores, limit top chunks, and collect IDs/previews.

### 75. Why use streams in `DocumentService`?
Because ranking and transforming top chunk data is a natural fit for map-sort-limit-collect style processing.

### 76. What is `Optional` in Java?
`Optional` models possibly absent values. In this project, `repository.findById(...)` returns an `Optional` that is handled with `orElseThrow(...)`.

### 77. Why is `Optional` useful in repositories?
It makes missing database records explicit and avoids silent null handling.

### 78. What is a record in Java?
A record is a compact immutable data carrier. `DocumentProcessingPipeline` uses `record LineRange(int startLine, int endLine)`.

### 79. Why is the `LineRange` record a good use of records?
It represents a small immutable value object with no extra ceremony, which is exactly what records are good for.

### 80. What is `var` and is it used here?
`var` is local type inference. This codebase mostly sticks with explicit types, which improves readability in service-layer business code.

## Section 8: Concurrency and Async Basics

### 81. What is a thread?
A thread is a unit of execution. The backend uses async execution so document processing can run separately from the HTTP request thread.

### 82. What is asynchronous processing?
Async processing means work continues in the background without blocking the caller. `DocumentProcessingWorker.processDocumentAsync(...)` is the main example.

### 83. How is async enabled in this codebase?
`DocumentServiceApplication` uses `@EnableAsync`, and `DocumentProcessingWorker` uses `@Async`.

### 84. Why use async processing for document upload?
PDF extraction, chunking, and embedding generation can take time. Returning `202 Accepted` keeps the API responsive.

### 85. What is a potential concurrency concern in systems like this?
Long-running downstream calls can consume threads and capacity. That is why the project now also uses timeout and bulkhead protections around outbound calls.

### 86. What is a bulkhead in code terms?
A bulkhead isolates concurrency for specific dependency types. In this project it protects AI and security outbound calls from exhausting shared capacity.

### 87. What is a timeout in concurrency terms?
A timeout ensures code does not wait forever. This protects worker threads and request threads from hanging on slow external systems.

### 88. Why is fail-fast behavior useful in backend services?
It prevents thread starvation, improves predictability, and allows the system to degrade gracefully instead of stalling.

## Section 9: I/O, JSON, and Integration Basics

### 89. How does this codebase handle file I/O?
Uploaded PDFs are read using `MultipartFile.getBytes()` in `DocumentService`, then processed in memory by the pipeline.

### 90. How does this codebase process PDF content?
`PdfService` extracts text, after which `ChunkService` splits the content into chunks for embedding and retrieval.

### 91. What is JSON mapping in Java?
It is conversion between Java objects and JSON. This project uses Jackson `ObjectMapper` to parse and generate JSON for AI requests and responses.

### 92. Why use `ObjectMapper` here?
Because the service integrates with APIs that speak JSON and returns structured JSON payloads to callers.

### 93. What is serialization?
Serialization is converting data into a transport/storage format. Embedding vectors are serialized to JSON strings before being stored in the database.

### 94. What is deserialization?
Deserialization is converting stored or received structured data back into Java objects. Stored embedding JSON is deserialized into `List<Double>`.

### 95. Why store embeddings as JSON strings here?
It is a simple persistence strategy that works without introducing a dedicated vector database or specialized column type.

## Section 10: Enterprise Java and Spring-Friendly Concepts

### 96. What is dependency injection?
Dependency injection means providing required collaborators from the outside instead of creating them internally. This project uses constructor-based DI throughout Spring-managed services.

### 97. Why is dependency injection good for interviews to mention?
Because it improves modularity, testability, and separation of concerns. This codebase’s services are easy to unit test because dependencies are injected.

### 98. What is an interface-based repository in Java enterprise code?
A repository interface expresses persistence behavior without manual implementation. `DocumentRepository extends JpaRepository<Document, Long>` is the clearest example.

### 99. What is the benefit of separating controller, service, and repository layers?
It keeps transport, business logic, and persistence concerns isolated. This codebase follows that structure consistently, which makes it easier to maintain and explain.

### 100. If asked “How do you show strong Java fundamentals from a real project?”, what should you say?
You can say this codebase demonstrates core Java through OOP, encapsulation, inheritance, enums, exceptions, collections, generics, streams, records, async execution, JSON mapping, and clean layering. Then point to concrete examples such as `DocumentService`, `JwtAuthenticationFilter`, `Role`, `DocumentProcessingException`, `ChunkService`, and `DocumentProcessingPipeline`.

## Strong Short Summary for Interviews

If you need a concise closing summary, use this:

"In this project I used core Java in practical backend scenarios: classes and objects for domain modeling, constructor-based dependency injection, inheritance in filters, enums for roles, custom runtime exceptions for domain failures, lists and maps for API and embedding workflows, streams for ranking and transformation, records for lightweight immutable value objects, and async execution for non-blocking document processing. The value is that these were not academic examples; they were used in real service, controller, security, and persistence code."

## Best Files to Review Before an Interview

- `src/main/java/com/policymind/document/service/DocumentService.java`
- `src/main/java/com/policymind/document/service/DocumentProcessingPipeline.java`
- `src/main/java/com/policymind/document/service/ChunkService.java`
- `src/main/java/com/policymind/document/service/OpenAiService.java`
- `src/main/java/com/policymind/document/service/EmbeddingService.java`
- `src/main/java/com/policymind/document/security/JwtAuthenticationFilter.java`
- `src/main/java/com/policymind/document/security/SecurityConfig.java`
- `src/main/java/com/policymind/document/entity/User.java`
- `src/main/java/com/policymind/document/entity/DocumentChunk.java`
- `src/main/java/com/policymind/document/enums/Role.java`
- `src/main/java/com/policymind/document/exception/DocumentProcessingException.java`
