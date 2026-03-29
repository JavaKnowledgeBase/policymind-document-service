package com.policymind.document.service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ChunkServiceTest {

    @Test
    public void extractClauseChunks_buildsStructuredPolicyClauses() {
        DocumentChunkRepository repo = mock(DocumentChunkRepository.class);
        ChunkService svc = new ChunkService(repo);

        String text = "REMOTE WORK POLICY\n\nPurpose\nEmployees may work remotely with manager approval.\n\nEligibility\n- Employees must maintain secure access.\n- Managers must review requests.";

        List<ChunkService.ClauseChunk> chunks = svc.extractClauseChunks(text, "remote-work.pdf");

        assertEquals(3, chunks.size());
        assertEquals("Purpose", chunks.get(0).sectionTitle());
        assertEquals("purpose", chunks.get(0).clauseType());
        assertEquals("Remote Work Policy", chunks.get(0).policyType());
        assertEquals("policy_clause", chunks.get(0).chunkKind());
        assertTrue(chunks.get(1).content().contains("maintain secure access"));
        assertTrue(chunks.get(1).riskTags().contains("mandatory_language"));
    }
}
