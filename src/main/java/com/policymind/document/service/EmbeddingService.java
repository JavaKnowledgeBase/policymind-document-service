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

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymind.document.dto.EmbeddingRequest;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {
	
    	private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
	
	    @Value("${openai.api.key}")
	    private String apiKey;

        @Value("${ai.provider.embedding:openai}")
        private String defaultEmbeddingProvider;

        private final VertexAiService vertexAiService;

	    private final RestTemplate restTemplate = new RestTemplate();
        private final ObjectMapper objectMapper = new ObjectMapper();

        public EmbeddingService(VertexAiService vertexAiService) {
            this.vertexAiService = vertexAiService;
        }
	    
	    /**
	     * Calls OpenAI Embedding API to convert text into a semantic vector.
	     *
	     * Embeddings represent meaning numerically in high-dimensional space.
	     * These vectors allow semantic similarity comparison.
	     *
	     * Why this exists:
	     * Enables ranking document sections based on relevance to user queries.
	     *
	     * @param text Input text to embed.
	     * @return Raw JSON response containing embedding vector.
	     */
	    
	    public List<Double> generateEmbedding(String text) {
            return generateEmbedding(text, null);
        }

        public List<Double> generateEmbedding(String text, String providerOverride) {
            String provider = resolveProvider(providerOverride);
            if ("vertex".equals(provider)) {
                return vertexAiService.generateEmbedding(text);
            }

	        String url = "https://api.openai.com/v1/embeddings";

	        HttpHeaders headers = new HttpHeaders();
	        headers.setBearerAuth(apiKey);
	        headers.setContentType(MediaType.APPLICATION_JSON);

	        EmbeddingRequest request =
	                new EmbeddingRequest("text-embedding-3-small", text);

	        String body;
			try {
				body = objectMapper.writeValueAsString(request);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to build embedding request", e);
			}

	        HttpEntity<String> httpRequest = new HttpEntity<>(body, headers);

	        ResponseEntity<String> response =
	                restTemplate.postForEntity(url, httpRequest, String.class);

	        return extractVector(response.getBody());
	    }

        private String resolveProvider(String providerOverride) {
            String raw = providerOverride == null || providerOverride.isBlank()
                    ? defaultEmbeddingProvider
                    : providerOverride;
            if (raw == null) {
                return "openai";
            }
            return raw.trim().toLowerCase();
        }

        public String serializeEmbedding(List<Double> vector) {
            try {
                return objectMapper.writeValueAsString(vector);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize embedding vector", e);
            }
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
