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


@Service
public class EmbeddingService {
	
    	private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
	
	    @Value("${openai.api.key}")
	    private String apiKey;

	    private final RestTemplate restTemplate = new RestTemplate();
	    
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
	    
	    public String generateEmbedding(String text) {

	        String url = "https://api.openai.com/v1/embeddings";

	        HttpHeaders headers = new HttpHeaders();
	        headers.setBearerAuth(apiKey);
	        headers.setContentType(MediaType.APPLICATION_JSON);

	        ObjectMapper objectMapper = new ObjectMapper();

	        EmbeddingRequest request =
	                new EmbeddingRequest("text-embedding-3-small", text);

	        String body = "";
			try {
				body = objectMapper.writeValueAsString(request);
			} catch (JsonProcessingException e) {
				logger.error("Failed to process request due to error", e);
				//e.printStackTrace();
			}

	        HttpEntity<String> httpRequest = new HttpEntity<>(body, headers);

	        ResponseEntity<String> response =
	                restTemplate.postForEntity(url, httpRequest, String.class);

	        return response.getBody();
	    }

}
