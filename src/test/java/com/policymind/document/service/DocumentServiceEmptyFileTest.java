package com.policymind.document.service;

import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.repository.DocumentRepository;
import com.policymind.document.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceEmptyFileTest {

    @Mock
    DocumentRepository repository;

    @Mock
    ChunkService chunkService;

    @Mock
    DocumentChunkRepository chunkRepository;

    @Mock
    EmbeddingService embeddingService;

    @Mock
    OpenAiService openAiService;

    @Mock
    VertexAiService vertexAiService;

    @Mock
    PdfService pdfService;

    @InjectMocks
    DocumentService documentService;

    @Test
    public void processDocument_emptyFile_throws() {
        MockMultipartFile empty = new MockMultipartFile("file","empty.pdf","application/pdf", new byte[0]);

        assertThrows(DocumentProcessingException.class, () -> documentService.processDocument(empty));
    }
}
