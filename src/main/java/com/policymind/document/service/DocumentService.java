package com.policymind.document.service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;
import com.policymind.document.repository.DocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repository;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final OpenAiService openAiService;
    private final VertexAiService vertexAiService;
    private final DocumentProcessingWorker documentProcessingWorker;
    private final DocumentProcessingPipeline documentProcessingPipeline;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentService(DocumentRepository repository,
                           DocumentChunkRepository chunkRepository,
                           EmbeddingService embeddingService,
                           OpenAiService openAiService,
                           VertexAiService vertexAiService,
                           DocumentProcessingWorker documentProcessingWorker,
                           DocumentProcessingPipeline documentProcessingPipeline) {
        this.repository = repository;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.openAiService = openAiService;
        this.vertexAiService = vertexAiService;
        this.documentProcessingWorker = documentProcessingWorker;
        this.documentProcessingPipeline = documentProcessingPipeline;
    }

    public Map<String, Object> processDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException("Uploaded file is empty.");
        }

        Document savedDoc = createProcessingDocument(file.getOriginalFilename(), "PROCESSING");
        return documentProcessingPipeline.processStoredDocument(savedDoc.getId(), savedDoc.getFileName(), readFileBytes(file));
    }

    public Map<String, Object> submitDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException("Uploaded file is empty.");
        }

        Document savedDoc = createProcessingDocument(file.getOriginalFilename(), "QUEUED");
        logger.info("Document accepted for async processing, documentId={}, file={}", savedDoc.getId(), savedDoc.getFileName());
        documentProcessingWorker.processDocumentAsync(savedDoc.getId(), savedDoc.getFileName(), readFileBytes(file));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Document accepted for processing.");
        response.put("documentId", savedDoc.getId());
        response.put("fileName", savedDoc.getFileName());
        response.put("status", savedDoc.getStatus());
        response.put("statusUrl", "/documents/" + savedDoc.getId());
        return response;
    }

    public Map<String, Object> getDocumentStatus(Long documentId) {
        Document document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));
        long chunksStored = chunkRepository.countByDocumentId(documentId);
        logger.debug("Document status requested, documentId={}, status={}, chunksStored={}", documentId, document.getStatus(), chunksStored);

        Map<String, Object> response = new HashMap<>();
        response.put("documentId", document.getId());
        response.put("fileName", document.getFileName());
        response.put("status", document.getStatus());
        response.put("createdAt", document.getCreatedAt());
        response.put("updatedAt", document.getUpdatedAt());
        response.put("completedAt", document.getCompletedAt());
        response.put("errorMessage", document.getErrorMessage());
        response.put("chunksStored", chunksStored);
        return response;
    }

    public Map<String, Object> askQuestion(Long documentId, String question, String embeddingProvider, String answerProvider) {
        List<DocumentChunk> chunks = chunkRepository.findByDocumentId(documentId);

        if (chunks.isEmpty()) {
            return Map.of("error", "No chunks found for this document.");
        }

        List<Double> questionEmbedding = embeddingService.generateEmbedding(question, embeddingProvider);
        Map<DocumentChunk, Double> similarityScores = new HashMap<>();

        for (DocumentChunk chunk : chunks) {
            if (chunk.getEmbedding() == null) {
                continue;
            }

            List<Double> chunkEmbedding = embeddingService.deserializeEmbedding(chunk.getEmbedding());
            if (chunkEmbedding.isEmpty()) {
                continue;
            }

            double similarity = cosineSimilarity(questionEmbedding, chunkEmbedding);
            similarityScores.put(chunk, similarity);
        }

        if (similarityScores.isEmpty()) {
            return Map.of("error", "No usable embeddings found for this document.");
        }

        List<DocumentChunk> topChunks = similarityScores.entrySet()
                .stream()
                .sorted(Map.Entry.<DocumentChunk, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        StringBuilder context = new StringBuilder();
        for (DocumentChunk chunk : topChunks) {
            context.append("[")
                    .append(toLineRangeLabel(chunk))
                    .append(" | Chunk ")
                    .append(chunk.getId())
                    .append("]\n")
                    .append(chunk.getContent())
                    .append("\n\n");
        }

        String normalizedAnswerProvider = normalizeAnswerProvider(answerProvider);

        Map<String, Object> providerResponses = new HashMap<>();
        Map<String, Object> openAiStructured = null;
        Map<String, Object> vertexStructured = null;

        if ("openai".equals(normalizedAnswerProvider) || "both".equals(normalizedAnswerProvider)) {
            String openAiResponseJson = openAiService.askLLM(context.toString(), question);
            openAiStructured = parseStructuredAnswer(openAiResponseJson);
            providerResponses.put("openai", openAiStructured);
        }

        if ("vertex".equals(normalizedAnswerProvider) || "both".equals(normalizedAnswerProvider)) {
            String vertexResponseJson = vertexAiService.askLLM(context.toString(), question);
            vertexStructured = parseStructuredAnswer(vertexResponseJson);
            providerResponses.put("vertex", vertexStructured);
        }

        Map<String, Object> structuredAnswer = openAiStructured != null ? openAiStructured : vertexStructured;
        if (structuredAnswer == null) {
            structuredAnswer = Map.of(
                    "summary", "No answer provider configured",
                    "answer", "Set answer provider to openai, vertex, or both.",
                    "confidence", "low",
                    "risk_score", 5,
                    "key_risks", List.of(),
                    "recommended_actions", List.of()
            );
        }

        List<Long> chunkIds = topChunks.stream()
                .map(DocumentChunk::getId)
                .collect(Collectors.toList());
        List<String> chunkLineRanges = topChunks.stream()
                .map(this::toLineRangeLabel)
                .collect(Collectors.toList());
        List<String> chunkPreviews = topChunks.stream()
                .map(chunk -> trimPreview(chunk.getContent()))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("documentId", documentId);
        response.put("question", question);
        response.put("embeddingProvider", normalizeEmbeddingProvider(embeddingProvider));
        response.put("answerProvider", normalizedAnswerProvider);
        response.put("retrievedChunkIds", chunkIds);
        response.put("retrievedLineRanges", chunkLineRanges);
        response.put("retrievedChunkPreviews", chunkPreviews);
        response.put("providers", providerResponses);
        response.put("structuredOutput", structuredAnswer);
        return response;
    }

    public Map<String, Object> askQuestion(Long documentId, String question) {
        return askQuestion(documentId, question, null, null);
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        int size = Math.min(v1.size(), v2.size());
        if (size == 0) {
            return 0.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < size; i++) {
            dot += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dot / denominator;
    }

    private Map<String, Object> parseStructuredAnswer(String llmResponseJson) {
        try {
            return objectMapper.readValue(llmResponseJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("summary", "Failed to parse structured output");
            fallback.put("answer", "Raw LLM output is available for debugging.");
            fallback.put("confidence", "low");
            fallback.put("risk_score", 5);
            fallback.put("key_risks", new ArrayList<>());
            fallback.put("recommended_actions", new ArrayList<>());
            fallback.put("raw", llmResponseJson);
            return fallback;
        }
    }

    private String trimPreview(String text) {
        int limit = 250;
        if (text == null) {
            return "";
        }
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private String toLineRangeLabel(DocumentChunk chunk) {
        Integer startLine = chunk.getStartLine();
        Integer endLine = chunk.getEndLine();
        if (startLine == null || endLine == null) {
            return "Line unavailable";
        }
        if (startLine.equals(endLine)) {
            return "Line " + startLine;
        }
        return "Lines " + startLine + "-" + endLine;
    }

    private String normalizeEmbeddingProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "openai";
        }
        return provider.trim().toLowerCase();
    }

    private String normalizeAnswerProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "openai";
        }
        String normalized = provider.trim().toLowerCase();
        if ("openai".equals(normalized) || "vertex".equals(normalized) || "both".equals(normalized)) {
            return normalized;
        }
        return "openai";
    }

    private Document createProcessingDocument(String fileName, String status) {
        Document document = new Document();
        document.setFileName(fileName);
        document.setStatus(status);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        Document savedDocument = repository.save(document);
        logger.info("Document record created, documentId={}, file={}, status={}", savedDocument.getId(), savedDocument.getFileName(), savedDocument.getStatus());
        return savedDocument;
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read uploaded file bytes.", e);
        }
    }
}
