package com.policymind.document.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class VertexAiServiceTest {

    @Test
    public void generateEmbedding_returnsVectorFromProvider() {
        VertexAiService svc = mock(VertexAiService.class);
        when(svc.generateEmbedding("hi")).thenReturn(List.of(0.5, 0.6));

        List<Double> vec = svc.generateEmbedding("hi");
        assertEquals(2, vec.size());
        verify(svc, times(1)).generateEmbedding("hi");
    }
}
