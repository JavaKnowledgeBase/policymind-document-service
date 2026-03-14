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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final PdfService pdfService;
    private final DocumentRepository repository;
    private final ChunkService chunkService;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final OpenAiService openAiService;
    private final VertexAiService vertexAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentService(DocumentRepository repository,
                           ChunkService chunkService,
                           DocumentChunkRepository chunkRepository,
                           EmbeddingService embeddingService,
                           OpenAiService openAiService,
                           VertexAiService vertexAiService,
                           PdfService pdfService) {
        this.pdfService = pdfService;
        this.repository = repository;
        this.chunkService = chunkService;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.openAiService = openAiService;
        this.vertexAiService = vertexAiService;
    }

    public Map<String, Object> processDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException("Uploaded file is empty.");
        }

        Document savedDoc = new Document();
        String stage = "initialize";

        try {
            stage = "persist document metadata";
            savedDoc.setFileName(file.getOriginalFilename());
            savedDoc.setStatus("PROCESSING");
            savedDoc.setCreatedAt(LocalDateTime.now());
            savedDoc = repository.save(savedDoc);

            stage = "extract PDF text";
            String text = pdfService.extractText(file);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("No readable text extracted from the uploaded file.");
            }

            stage = "chunk extracted text";
            List<String> chunks = chunkService.chunkText(text);
            if (chunks == null || chunks.isEmpty()) {
                throw new IllegalStateException("No text chunks generated from extracted content.");
            }
            List<LineRange> chunkLineRanges = buildChunkLineRanges(text, chunks, chunkService.getChunkSize());

            stage = "generate embeddings and persist chunks";
            int chunkCount = 0;
            for (int idx = 0; idx < chunks.size(); idx++) {
                String chunkText = chunks.get(idx);
                List<Double> embeddingVector = embeddingService.generateEmbedding(chunkText);
                LineRange lineRange = chunkLineRanges.get(idx);

                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocument(savedDoc);
                chunk.setContent(chunkText);
                chunk.setEmbedding(embeddingService.serializeEmbedding(embeddingVector));
                chunk.setStartLine(lineRange.startLine());
                chunk.setEndLine(lineRange.endLine());
                chunkRepository.save(chunk);
                chunkCount++;
            }

            stage = "mark document completed";
            savedDoc.setStatus("COMPLETED");
            repository.save(savedDoc);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document uploaded and processed successfully.");
            response.put("documentId", savedDoc.getId());
            response.put("fileName", savedDoc.getFileName());
            response.put("status", savedDoc.getStatus());
            response.put("chunksStored", chunkCount);
            return response;
        } catch (Exception e) {
            logger.error(
                    "Document processing failed at stage='{}', documentId='{}', file='{}'",
                    stage,
                    savedDoc.getId(),
                    savedDoc.getFileName(),
                    e
            );

            if (savedDoc.getId() != null) {
                try {
                    savedDoc.setStatus("FAILED");
                    repository.save(savedDoc);
                } catch (Exception statusEx) {
                    logger.error("Failed to update document status to FAILED for id={}", savedDoc.getId(), statusEx);
                }
            }

            throw new DocumentProcessingException(
                    "Failed to process document at stage '" + stage + "': " + rootCauseMessage(e),
                    e
            );
        }
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

    private List<LineRange> buildChunkLineRanges(String fullText, List<String> chunks, int chunkSize) {
        List<LineRange> ranges = new ArrayList<>();
        int offset = 0;

        for (String chunk : chunks) {
            int safeChunkSize = chunk == null ? 0 : chunk.length();
            int startOffset = Math.min(offset, fullText.length());
            int endOffset = Math.min(fullText.length(), startOffset + Math.max(safeChunkSize, 0));

            int startLine = lineNumberAtOffset(fullText, startOffset);
            int endLine = lineNumberAtOffset(fullText, Math.max(startOffset, endOffset));
            ranges.add(new LineRange(startLine, Math.max(startLine, endLine)));

            offset += Math.min(chunkSize, Math.max(safeChunkSize, 0));
        }

        return ranges;
    }

    private int lineNumberAtOffset(String text, int offsetExclusive) {
        if (text == null || text.isEmpty()) {
            return 1;
        }

        int line = 1;
        int safeOffset = Math.min(Math.max(offsetExclusive, 0), text.length());
        for (int i = 0; i < safeOffset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
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

    private record LineRange(int startLine, int endLine) {
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
