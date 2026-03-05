package com.policymind.document.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EmbeddingServiceTest {

    @Test
    public void serializeAndDeserialize() {
        VertexAiService vertex = mock(VertexAiService.class);
        EmbeddingService svc = new EmbeddingService(vertex);

        List<Double> vector = List.of(1.0, 2.5, -3.0);
        String json = svc.serializeEmbedding(vector);
        assertNotNull(json);

        List<Double> parsed = svc.deserializeEmbedding(json);
        assertEquals(3, parsed.size());
        assertEquals(2.5, parsed.get(1));
    }

    @Test
    public void generateEmbedding_delegatesToVertex_whenProviderIsVertex() {
        VertexAiService vertex = mock(VertexAiService.class);
        when(vertex.generateEmbedding("hello")).thenReturn(List.of(0.1, 0.2));

        EmbeddingService svc = new EmbeddingService(vertex);
        List<Double> result = svc.generateEmbedding("hello", "vertex");

        assertEquals(2, result.size());
        verify(vertex, times(1)).generateEmbedding("hello");
    }

    @Test
    public void deserializeEmbedding_invalidJson_returnsEmpty() {
        VertexAiService vertex = mock(VertexAiService.class);
        EmbeddingService svc = new EmbeddingService(vertex);

        List<Double> parsed = svc.deserializeEmbedding("not-json");
        assertTrue(parsed.isEmpty());
    }
}
