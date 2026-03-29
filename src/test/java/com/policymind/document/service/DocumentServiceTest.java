package com.policymind.document.service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;
import com.policymind.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock DocumentChunkRepository chunkRepository;
    @Mock EmbeddingService embeddingService;
    @Mock OpenAiService openAiService;
    @Mock VertexAiService vertexAiService;
    @Mock DocumentProcessingWorker documentProcessingWorker;
    @Mock DocumentProcessingPipeline documentProcessingPipeline;
    @Mock TrustedPolicyReferenceService trustedPolicyReferenceService;

    @InjectMocks DocumentService documentService;

    @Test
    public void processDocument_validPdf_delegatesToPipeline() {
        MockMultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", new byte[] {1, 2, 3});
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
    public void reviewDocument_returnsMissingAndRiskyClauses() {
        Document document = new Document();
        document.setId(8L);
        document.setFileName("remote-work.pdf");
        document.setStatus("COMPLETED");

        DocumentChunk purpose = new DocumentChunk();
        purpose.setId(101L);
        purpose.setClauseType("purpose");
        purpose.setSectionTitle("Purpose");
        purpose.setContent("Remote work gives employees flexibility.");
        purpose.setRiskTags("approval_dependency");
        purpose.setPolicyType("Remote Work Policy");
        purpose.setStartLine(3);
        purpose.setEndLine(4);

        DocumentChunk scope = new DocumentChunk();
        scope.setId(102L);
        scope.setClauseType("scope");
        scope.setSectionTitle("Scope");
        scope.setContent("This policy applies to all employees.");
        scope.setRiskTags("mandatory_language");
        scope.setPolicyType("Remote Work Policy");
        scope.setStartLine(5);
        scope.setEndLine(5);

        when(documentRepository.findById(8L)).thenReturn(Optional.of(document));
        when(chunkRepository.findByDocumentId(8L)).thenReturn(List.of(purpose, scope));
        when(trustedPolicyReferenceService.getHrInternalPolicyReferences()).thenReturn(List.of(
                new TrustedPolicyReferenceService.ReferenceClause("Purpose", "purpose", "HR Policy", "Purpose text", "PolicyMind HR Starter Library", "mandatory_language"),
                new TrustedPolicyReferenceService.ReferenceClause("Scope", "scope", "HR Policy", "Scope text", "PolicyMind HR Starter Library", "mandatory_language"),
                new TrustedPolicyReferenceService.ReferenceClause("Approval Workflow", "approval", "HR Policy", "Approval text", "PolicyMind HR Starter Library", "mandatory_language,approval_dependency")
        ));

        Map<String, Object> response = documentService.reviewDocument(8L);

        assertEquals(8L, response.get("documentId"));
        assertEquals("Remote Work Policy", response.get("policyType"));
        Map<String, Object> summary = (Map<String, Object>) response.get("summary");
        assertEquals("Needs revision", summary.get("assessment"));
        assertTrue(String.valueOf(summary.get("overview")).contains("needs revision"));
        assertTrue(String.valueOf(summary.get("documentText")).contains("Remote work gives employees flexibility."));
        assertTrue(((List<?>) response.get("missingClauses")).size() >= 1);
        assertTrue(((List<?>) response.get("riskyClauses")).size() >= 1);
        assertTrue(((List<?>) response.get("suggestedClauses")).size() >= 1);
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
        when(openAiService.askLLM(anyString(), anyString())).thenReturn("{\"summary\":\"ok\",\"answer\":\"yes\",\"confidence\":\"high\",\"risk_score\":1,\"key_risks\":[],\"recommended_actions\":[]}");

        Map<String, Object> resp = documentService.askQuestion(2L, "Does this work?", null, "openai");
        assertEquals(2L, resp.get("documentId"));
        assertEquals("Does this work?", resp.get("question"));
        assertEquals("openai", resp.get("answerProvider"));
        Map<String, Object> structured = (Map<String, Object>) resp.get("structuredOutput");
        assertEquals("ok", structured.get("summary"));
    }

    @Test
    public void getDocumentStatus_returnsStatusPayload() {
        Document document = new Document();
        document.setId(5L);
        document.setFileName("policy.pdf");
        document.setStatus("PROCESSING");

        when(documentRepository.findById(5L)).thenReturn(Optional.of(document));
        when(chunkRepository.countByDocumentId(5L)).thenReturn(2L);

        Map<String, Object> response = documentService.getDocumentStatus(5L);
        assertEquals(5L, response.get("documentId"));
        assertEquals("policy.pdf", response.get("fileName"));
        assertEquals("PROCESSING", response.get("status"));
        assertEquals(2L, response.get("chunksStored"));
    }
}
