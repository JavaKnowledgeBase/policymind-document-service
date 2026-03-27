package com.policymind.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EmbeddingServiceTest {

    @Test
    public void serializeAndDeserialize() {
        VertexAiService vertex = mock(VertexAiService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        OutboundCallExecutor outboundCallExecutor = mock(OutboundCallExecutor.class);
        EmbeddingService svc = new EmbeddingService("test-key", "openai", vertex, restTemplate, outboundCallExecutor, new ObjectMapper());

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

        RestTemplate restTemplate = mock(RestTemplate.class);
        OutboundCallExecutor outboundCallExecutor = mock(OutboundCallExecutor.class);
        EmbeddingService svc = new EmbeddingService("test-key", "openai", vertex, restTemplate, outboundCallExecutor, new ObjectMapper());
        List<Double> result = svc.generateEmbedding("hello", "vertex");

        assertEquals(2, result.size());
        verify(vertex).generateEmbedding("hello");
    }

    @Test
    public void deserializeEmbedding_invalidJson_returnsEmpty() {
        VertexAiService vertex = mock(VertexAiService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        OutboundCallExecutor outboundCallExecutor = mock(OutboundCallExecutor.class);
        EmbeddingService svc = new EmbeddingService("test-key", "openai", vertex, restTemplate, outboundCallExecutor, new ObjectMapper());

        List<Double> parsed = svc.deserializeEmbedding("not-json");
        assertTrue(parsed.isEmpty());
    }

    @Test
    public void generateEmbedding_usesOpenAiPath_whenProviderIsDefault() {
        VertexAiService vertex = mock(VertexAiService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        OutboundCallExecutor outboundCallExecutor = mock(OutboundCallExecutor.class);
        EmbeddingService svc = new EmbeddingService("test-key", "openai", vertex, restTemplate, outboundCallExecutor, new ObjectMapper());

        String fake = "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}";
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(fake));
        when(outboundCallExecutor.execute(eq("openai-embedding"), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Supplier<List<Double>> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        List<Double> result = svc.generateEmbedding("hello");

        assertEquals(3, result.size());
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        verifyNoInteractions(vertex);
    }
}
