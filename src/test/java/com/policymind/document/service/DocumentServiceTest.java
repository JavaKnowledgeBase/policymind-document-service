package com.policymind.document.service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;
import com.policymind.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock
    DocumentRepository documentRepository;

    @Mock
    DocumentChunkRepository chunkRepository;

    @Mock
    EmbeddingService embeddingService;

    @Mock
    OpenAiService openAiService;

    @Mock
    VertexAiService vertexAiService;

    @Mock
    DocumentProcessingWorker documentProcessingWorker;

    @Mock
    DocumentProcessingPipeline documentProcessingPipeline;

    @InjectMocks
    DocumentService documentService;

    @BeforeEach
    public void setup() {
        // default behavior
    }

    @Test
    public void processDocument_validPdf_delegatesToPipeline() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.pdf",
                "application/pdf",
                new byte[] {1, 2, 3}
        );

        Document savedDocument = new Document();
        savedDocument.setId(11L);
        savedDocument.setFileName("policy.pdf");
        savedDocument.setStatus("PROCESSING");

        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(documentProcessingPipeline.processStoredDocument(11L, "policy.pdf", new byte[] {1, 2, 3}))
                .thenReturn(Map.of("documentId", 11L, "status", "COMPLETED", "chunksStored", 1));

        Map<String, Object> response = documentService.processDocument(file);

        assertEquals(11L, response.get("documentId"));
        assertEquals("COMPLETED", response.get("status"));
        verify(documentProcessingPipeline).processStoredDocument(11L, "policy.pdf", new byte[] {1, 2, 3});
    }

    @Test
    public void askQuestion_noChunks_returnsError() {
        when(embeddingService.resolveProvider(null)).thenReturn("openai");
        when(embeddingService.generateEmbedding(anyString(), any())).thenReturn(List.of(1.0, 1.0, 1.0));
        when(embeddingService.supportsPgVector("openai")).thenReturn(true);
        when(embeddingService.toPgVectorLiteral(List.of(1.0, 1.0, 1.0))).thenReturn("[1.0,1.0,1.0]");
        when(chunkRepository.findTopSimilarChunks(1L, "[1.0,1.0,1.0]", 3)).thenReturn(List.of());
        when(chunkRepository.findByDocumentId(1L)).thenReturn(List.of());

        Map<String, Object> resp = documentService.askQuestion(1L, "what?");
        assertTrue(resp.containsKey("error"));
        assertEquals("No usable embeddings found for this document.", resp.get("error"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void askQuestion_withPgVectorChunks_openaiProvider_returnsStructured() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(10L);
        chunk.setContent("This is a test chunk content that is fairly long.");
        chunk.setEmbedding("[1.0,1.0,1.0]");
        chunk.setStartLine(1);
        chunk.setEndLine(3);

        when(embeddingService.resolveProvider(null)).thenReturn("openai");
        when(embeddingService.generateEmbedding(anyString(), any())).thenReturn(List.of(1.0, 1.0, 1.0));
        when(embeddingService.supportsPgVector("openai")).thenReturn(true);
        when(embeddingService.toPgVectorLiteral(List.of(1.0, 1.0, 1.0))).thenReturn("[1.0,1.0,1.0]");
        when(chunkRepository.findTopSimilarChunks(2L, "[1.0,1.0,1.0]", 3)).thenReturn(List.of(chunk));

        String openaiJson = "{\"summary\":\"ok\",\"answer\":\"yes\",\"confidence\":\"high\",\"risk_score\":1,\"key_risks\":[],\"recommended_actions\":[]}";
        when(openAiService.askLLM(anyString(), anyString())).thenReturn(openaiJson);

        Map<String, Object> resp = documentService.askQuestion(2L, "Does this work?", null, "openai");

        assertEquals(2L, resp.get("documentId"));
        assertEquals("Does this work?", resp.get("question"));
        assertEquals("openai", resp.get("answerProvider"));

        assertTrue(resp.containsKey("structuredOutput"));
        Map<String, Object> structured = (Map<String, Object>) resp.get("structuredOutput");
        assertEquals("ok", structured.get("summary"));

        Map<String, Object> providers = (Map<String, Object>) resp.get("providers");
        assertTrue(providers.containsKey("openai"));
        verify(chunkRepository).findTopSimilarChunks(2L, "[1.0,1.0,1.0]", 3);
    }

    @Test
    public void getDocumentStatus_returnsStatusPayload() {
        Document document = new Document();
        document.setId(5L);
        document.setFileName("policy.pdf");
        document.setStatus("PROCESSING");

        when(documentRepository.findById(5L)).thenReturn(java.util.Optional.of(document));
        when(chunkRepository.countByDocumentId(5L)).thenReturn(2L);

        Map<String, Object> response = documentService.getDocumentStatus(5L);

        assertEquals(5L, response.get("documentId"));
        assertEquals("policy.pdf", response.get("fileName"));
        assertEquals("PROCESSING", response.get("status"));
        assertEquals(2L, response.get("chunksStored"));
    }
}
