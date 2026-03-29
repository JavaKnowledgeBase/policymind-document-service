package com.policymind.document.service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;
import com.policymind.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DocumentProcessingPipelineTest {

    @Mock PdfService pdfService;
    @Mock DocumentRepository repository;
    @Mock ChunkService chunkService;
    @Mock DocumentChunkRepository chunkRepository;
    @Mock EmbeddingService embeddingService;
    @InjectMocks DocumentProcessingPipeline pipeline;

    @Test
    public void processStoredDocument_blankExtractedText_marksFailedWithHelpfulMessage() throws Exception {
        Document document = new Document();
        document.setId(42L);
        document.setFileName("scan.pdf");
        document.setStatus("QUEUED");
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        when(repository.findById(42L)).thenReturn(Optional.of(document));
        when(repository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pdfService.extractText(any(byte[].class))).thenReturn("   ");

        DocumentProcessingException ex = assertThrows(DocumentProcessingException.class,
                () -> pipeline.processStoredDocument(42L, "scan.pdf", new byte[] {1, 2, 3}));

        assertEquals("Failed to process document at stage 'extract PDF text': " + DocumentProcessingPipeline.NO_READABLE_TEXT_MESSAGE, ex.getMessage());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(repository, atLeastOnce()).save(documentCaptor.capture());
        Document failedDocument = documentCaptor.getAllValues().get(documentCaptor.getAllValues().size() - 1);
        assertEquals("FAILED", failedDocument.getStatus());
        assertEquals(DocumentProcessingPipeline.NO_READABLE_TEXT_MESSAGE, failedDocument.getErrorMessage());
    }

    @Test
    public void processStoredDocument_persistsStructuredChunkMetadataAndMarksCompleted() throws Exception {
        Document document = new Document();
        document.setId(7L);
        document.setFileName("policy.pdf");
        document.setStatus("QUEUED");
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        DocumentChunk savedChunk = new DocumentChunk();
        savedChunk.setId(101L);
        savedChunk.setDocument(document);
        savedChunk.setContent("Employees may work remotely with manager approval.");

        ChunkService.ClauseChunk clauseChunk = new ChunkService.ClauseChunk(
                "Employees may work remotely with manager approval.",
                3, 3, "policy_clause", "Purpose", "purpose", "hr_internal_policy",
                "Remote Work Policy", "United States", "policy.pdf", "approval_dependency,mandatory_language", false);

        when(repository.findById(7L)).thenReturn(Optional.of(document));
        when(repository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pdfService.extractText(any(byte[].class))).thenReturn("Remote work text");
        when(chunkService.extractClauseChunks(anyString(), anyString())).thenReturn(List.of(clauseChunk));
        when(embeddingService.generateEmbedding(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));
        when(embeddingService.serializeEmbedding(List.of(0.1, 0.2, 0.3))).thenReturn("[0.1,0.2,0.3]");
        when(embeddingService.toPgVectorLiteral(List.of(0.1, 0.2, 0.3))).thenReturn("[0.1,0.2,0.3]");
        when(chunkRepository.save(any(DocumentChunk.class))).thenReturn(savedChunk);

        var response = pipeline.processStoredDocument(7L, "policy.pdf", new byte[] {1, 2, 3});

        assertEquals("COMPLETED", response.get("status"));
        assertEquals(1, response.get("chunksStored"));
        verify(chunkRepository).updateEmbeddingVector(101L, "[0.1,0.2,0.3]");

        ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
        verify(chunkRepository).save(chunkCaptor.capture());
        DocumentChunk storedChunk = chunkCaptor.getValue();
        assertEquals("Purpose", storedChunk.getSectionTitle());
        assertEquals("purpose", storedChunk.getClauseType());
        assertEquals("hr_internal_policy", storedChunk.getDomain());
        assertEquals("Remote Work Policy", storedChunk.getPolicyType());
        assertEquals(3, storedChunk.getStartLine());
        assertEquals(3, storedChunk.getEndLine());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(repository, atLeastOnce()).save(documentCaptor.capture());
        Document completedDocument = documentCaptor.getAllValues().get(documentCaptor.getAllValues().size() - 1);
        assertEquals("COMPLETED", completedDocument.getStatus());
    }
}
