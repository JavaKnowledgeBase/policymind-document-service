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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class EmbeddingService {
	
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

	        String body = """
	        {
	          "model": "text-embedding-3-small",
	          "input": "%s"
	        }
	        """.formatted(text.replace("\"", "'"));

	        HttpEntity<String> request = new HttpEntity<>(body, headers);

	        ResponseEntity<String> response =
	                restTemplate.postForEntity(url, request, String.class);

	        return response.getBody();
	    }

}
