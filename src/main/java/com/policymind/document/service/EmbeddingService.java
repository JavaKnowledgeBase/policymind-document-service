/**
 * ==============================================================
 * PolicyMind AI - EmbeddingService
 * ==============================================================
 *
 * Converts text into high-dimensional semantic vectors
 * using OpenAI Embedding API.
 *
 * Embeddings allow:
 * - Semantic similarity comparison
 * - Relevance ranking
 * - Retrieval-Augmented Generation (RAG)
 *
 * This layer abstracts external AI provider.
 */

package com.policymind.document.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymind.document.config.NetworkClientFactory;
import com.policymind.document.dto.EmbeddingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    public static final String OPENAI_PROVIDER = "openai";
    public static final int OPENAI_EMBEDDING_DIMENSION = 1536;

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private final String apiKey;
    private final String defaultEmbeddingProvider;
    private final VertexAiService vertexAiService;
    private final OutboundCallExecutor outboundCallExecutor;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public EmbeddingService(@Value("${openai.api.key}") String apiKey,
                            @Value("${ai.provider.embedding:openai}") String defaultEmbeddingProvider,
                            VertexAiService vertexAiService,
                            NetworkClientFactory networkClientFactory,
                            OutboundCallExecutor outboundCallExecutor,
                            ObjectMapper objectMapper) {
        this(apiKey,
                defaultEmbeddingProvider,
                vertexAiService,
                networkClientFactory.createRestTemplate("openai-embedding"),
                outboundCallExecutor,
                objectMapper);
    }

    EmbeddingService(String apiKey,
                     String defaultEmbeddingProvider,
                     VertexAiService vertexAiService,
                     RestTemplate restTemplate,
                     OutboundCallExecutor outboundCallExecutor,
                     ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.defaultEmbeddingProvider = defaultEmbeddingProvider;
        this.vertexAiService = vertexAiService;
        this.restTemplate = restTemplate;
        this.outboundCallExecutor = outboundCallExecutor;
        this.objectMapper = objectMapper;
    }

    public List<Double> generateEmbedding(String text) {
        return generateEmbedding(text, null);
    }

    public List<Double> generateEmbedding(String text, String providerOverride) {
        String provider = resolveProvider(providerOverride);
        if ("vertex".equals(provider)) {
            return vertexAiService.generateEmbedding(text);
        }

        return outboundCallExecutor.execute("openai-embedding", () -> executeOpenAiEmbedding(text));
    }

    private List<Double> executeOpenAiEmbedding(String text) {
        String url = "https://api.openai.com/v1/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        EmbeddingRequest request = new EmbeddingRequest("text-embedding-3-small", text);

        String body;
        try {
            body = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build embedding request", e);
        }

        HttpEntity<String> httpRequest = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, httpRequest, String.class);

        return extractVector(response.getBody());
    }

    public String resolveProvider(String providerOverride) {
        String raw = providerOverride == null || providerOverride.isBlank()
                ? defaultEmbeddingProvider
                : providerOverride;
        if (raw == null) {
            return OPENAI_PROVIDER;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public boolean supportsPgVector(String providerOverride) {
        return OPENAI_PROVIDER.equals(resolveProvider(providerOverride));
    }

    public String serializeEmbedding(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize embedding vector", e);
        }
    }

    public String toPgVectorLiteral(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("Embedding vector is empty.");
        }
        return vector.stream()
                .map(value -> String.format(Locale.ROOT, "%s", value))
                .collect(Collectors.joining(",", "[", "]"));
    }

    public List<Double> deserializeEmbedding(String embeddingJson) {
        try {
            JsonNode root = objectMapper.readTree(embeddingJson);
            List<Double> vector = new ArrayList<>();
            if (!root.isArray()) {
                return vector;
            }
            for (JsonNode node : root) {
                vector.add(node.asDouble());
            }
            return vector;
        } catch (Exception e) {
            logger.warn("Failed to parse stored embedding JSON, skipping chunk");
            return List.of();
        }
    }

    private List<Double> extractVector(String apiJson) {
        try {
            JsonNode root = objectMapper.readTree(apiJson);
            JsonNode vectorNode = root.path("data").path(0).path("embedding");
            List<Double> vector = new ArrayList<>();
            for (JsonNode value : vectorNode) {
                vector.add(value.asDouble());
            }
            return vector;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding response", e);
        }
    }
}
