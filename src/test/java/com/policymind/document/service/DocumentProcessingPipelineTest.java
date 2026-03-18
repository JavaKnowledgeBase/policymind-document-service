package com.policymind.document.service;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DocumentProcessingPipelineTest {

    @Mock
    PdfService pdfService;

    @Mock
    DocumentRepository repository;

    @Mock
    ChunkService chunkService;

    @Mock
    DocumentChunkRepository chunkRepository;

    @Mock
    EmbeddingService embeddingService;

    @InjectMocks
    DocumentProcessingPipeline pipeline;

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

        DocumentProcessingException ex = assertThrows(
                DocumentProcessingException.class,
                () -> pipeline.processStoredDocument(42L, "scan.pdf", new byte[] {1, 2, 3})
        );

        assertEquals(
                "Failed to process document at stage 'extract PDF text': " + DocumentProcessingPipeline.NO_READABLE_TEXT_MESSAGE,
                ex.getMessage()
        );

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(repository, atLeastOnce()).save(documentCaptor.capture());
        Document failedDocument = documentCaptor.getAllValues().get(documentCaptor.getAllValues().size() - 1);
        assertEquals("FAILED", failedDocument.getStatus());
        assertEquals(DocumentProcessingPipeline.NO_READABLE_TEXT_MESSAGE, failedDocument.getErrorMessage());
    }
}
