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
import java.util.List;
import java.util.Map;

@Service
public class DocumentProcessingPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingPipeline.class);
    static final String NO_READABLE_TEXT_MESSAGE =
            "We could not find readable text in that PDF. It may be a scanned or image-only file, so please try a searchable PDF or run OCR first.";

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
        logger.info("processStoredDocument entered, documentId={}, file='{}', sizeBytes={}", documentId, fileName, fileBytes == null ? null : fileBytes.length);

        try {
            stage = "mark document processing";
            savedDoc.setFileName(fileName);
            savedDoc.setStatus("PROCESSING");
            savedDoc.setUpdatedAt(LocalDateTime.now());
            savedDoc.setErrorMessage(null);
            savedDoc = repository.save(savedDoc);

            stage = "extract PDF text";
            String text = pdfService.extractText(fileBytes);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException(NO_READABLE_TEXT_MESSAGE);
            }

            stage = "extract structured policy clauses";
            List<ChunkService.ClauseChunk> clauses = chunkService.extractClauseChunks(text, fileName);
            if (clauses == null || clauses.isEmpty()) {
                throw new IllegalStateException("No text chunks generated from extracted content.");
            }

            stage = "generate embeddings and persist chunks";
            int chunkCount = 0;
            for (ChunkService.ClauseChunk clause : clauses) {
                List<Double> embeddingVector = embeddingService.generateEmbedding(clause.content());
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocument(savedDoc);
                chunk.setContent(clause.content());
                chunk.setEmbedding(embeddingService.serializeEmbedding(embeddingVector));
                chunk.setStartLine(clause.startLine());
                chunk.setEndLine(clause.endLine());
                chunk.setChunkKind(clause.chunkKind());
                chunk.setSectionTitle(clause.sectionTitle());
                chunk.setClauseType(clause.clauseType());
                chunk.setDomain(clause.domain());
                chunk.setPolicyType(clause.policyType());
                chunk.setJurisdiction(clause.jurisdiction());
                chunk.setSourceName(clause.sourceName());
                chunk.setRiskTags(clause.riskTags());
                chunk.setReferenceClause(clause.referenceClause());
                chunk = chunkRepository.save(chunk);
                persistPgVector(chunk.getId(), embeddingVector);
                chunkCount++;
            }

            stage = "mark document completed";
            savedDoc.setStatus("COMPLETED");
            savedDoc.setUpdatedAt(LocalDateTime.now());
            savedDoc.setCompletedAt(LocalDateTime.now());
            savedDoc.setErrorMessage(null);
            repository.save(savedDoc);

            return Map.of("message", "Document uploaded and processed successfully.", "documentId", savedDoc.getId(), "fileName", savedDoc.getFileName(), "status", savedDoc.getStatus(), "chunksStored", chunkCount);
        } catch (Exception e) {
            logger.error("Document processing failed at stage='{}', documentId='{}', file='{}', sizeBytes={}", stage, savedDoc.getId(), savedDoc.getFileName(), fileBytes == null ? null : fileBytes.length, e);
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
            throw new DocumentProcessingException("Failed to process document at stage '" + stage + "': " + rootCauseMessage(e), e);
        }
    }

    private void persistPgVector(Long chunkId, List<Double> embeddingVector) {
        try {
            chunkRepository.updateEmbeddingVector(chunkId, embeddingService.toPgVectorLiteral(embeddingVector));
        } catch (Exception ex) {
            logger.warn("Chunk {} saved without pgvector column update. Falling back to JSON embeddings for retrieval.", chunkId, ex);
        }
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
