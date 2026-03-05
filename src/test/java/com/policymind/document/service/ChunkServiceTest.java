package com.policymind.document.service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChunkServiceTest {

    @Test
    public void chunkText_splitsCorrectly() {
        DocumentChunkRepository repo = mock(DocumentChunkRepository.class);
        ChunkService svc = new ChunkService(repo);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1800; i++) sb.append('a');
        List<String> chunks = svc.chunkText(sb.toString());
        assertEquals(3, chunks.size());
        assertEquals(800, chunks.get(0).length());
    }

    @Test
    public void saveChunks_truncatesLongChunk_andSaves() {
        DocumentChunkRepository repo = mock(DocumentChunkRepository.class);
        ChunkService svc = new ChunkService(repo);

        Document doc = new Document();
        StringBuilder longStr = new StringBuilder();
        for (int i = 0; i < 5000; i++) longStr.append('x');

        svc.saveChunks(doc, List.of(longStr.toString()));

        ArgumentCaptor<DocumentChunk> cap = ArgumentCaptor.forClass(DocumentChunk.class);
        verify(repo, times(1)).save(cap.capture());

        DocumentChunk saved = cap.getValue();
        assertNotNull(saved.getContent());
        assertTrue(saved.getContent().length() <= 3000);
        assertEquals(doc, saved.getDocument());
    }
}
