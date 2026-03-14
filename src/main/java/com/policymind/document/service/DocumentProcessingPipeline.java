package com.policymind.document.service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;
import com.policymind.document.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DocumentProcessingPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingPipeline.class);

    private final PdfService pdfService;
    private final DocumentRepository repository;
    private final ChunkService chunkService;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    public DocumentProcessingPipeline(PdfService pdfService,
                                      DocumentRepository repository,
                                      ChunkService chunkService,
                                      DocumentChunkRepository chunkRepository,
                                      EmbeddingService embeddingService) {
        this.pdfService = pdfService;
        this.repository = repository;
        this.chunkService = chunkService;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
    }

    public Map<String, Object> processStoredDocument(Long documentId, String fileName, byte[] fileBytes) {
        Document savedDoc = repository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));
        String stage = "initialize";

        try {
            stage = "mark document processing";
            savedDoc.setFileName(fileName);
            savedDoc.setStatus("PROCESSING");
            savedDoc.setUpdatedAt(LocalDateTime.now());
            savedDoc.setErrorMessage(null);
            savedDoc = repository.save(savedDoc);
            logger.info("Processing started for documentId={}, file={}", savedDoc.getId(), savedDoc.getFileName());

            stage = "extract PDF text";
            String text = pdfService.extractText(fileBytes);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("No readable text extracted from the uploaded file.");
            }
            logger.info("Text extraction completed for documentId={}, characters={}", savedDoc.getId(), text.length());

            stage = "chunk extracted text";
            List<String> chunks = chunkService.chunkText(text);
            if (chunks == null || chunks.isEmpty()) {
                throw new IllegalStateException("No text chunks generated from extracted content.");
            }
            logger.info("Chunking completed for documentId={}, chunkCount={}", savedDoc.getId(), chunks.size());
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
            logger.info("Embedding and persistence completed for documentId={}, chunksStored={}", savedDoc.getId(), chunkCount);

            stage = "mark document completed";
            savedDoc.setStatus("COMPLETED");
            savedDoc.setUpdatedAt(LocalDateTime.now());
            savedDoc.setCompletedAt(LocalDateTime.now());
            savedDoc.setErrorMessage(null);
            repository.save(savedDoc);
            logger.info("Processing completed for documentId={}, file={}", savedDoc.getId(), savedDoc.getFileName());

            return Map.of(
                    "message", "Document uploaded and processed successfully.",
                    "documentId", savedDoc.getId(),
                    "fileName", savedDoc.getFileName(),
                    "status", savedDoc.getStatus(),
                    "chunksStored", chunkCount
            );
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
                    savedDoc.setUpdatedAt(LocalDateTime.now());
                    savedDoc.setErrorMessage(rootCauseMessage(e));
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

    // The line references make the async pipeline observable from the UI and future worker services.
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

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record LineRange(int startLine, int endLine) {
    }
}
