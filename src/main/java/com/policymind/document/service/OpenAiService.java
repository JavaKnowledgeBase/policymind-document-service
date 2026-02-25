/**
 * ==============================================================
 * PolicyMind AI - OpenAiService
 * ==============================================================
 *
 * Handles interaction with OpenAI Chat Completion API.
 *
 * Includes:
 * - Structured request building
 * - Error handling
 * - Quota fallback mechanism
 *
 * This abstraction allows switching LLM providers
 * without modifying core business logic.
 */

package com.policymind.document.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenAiService {

	private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

	@Value("${openai.api.key}")
	private String apiKey;

	private final RestTemplate restTemplate = new RestTemplate();

	/**
	 * Sends contextual prompt to OpenAI Chat Completion API.
	 *
	 * The method: 1. Builds a structured message payload. 2. Sends context +
	 * question to LLM. 3. Handles quota or API errors gracefully. 4. Falls back to
	 * mock response if necessary.
	 *
	 * Why this exists: This is the final reasoning layer of the RAG system.
	 *
	 * @param context  Most relevant document chunks.
	 * @param question User's question.
	 * @return LLM-generated answer.
	 */

	public String askLLM(String context, String question) {

		try {

			String url = "https://api.openai.com/v1/chat/completions";

			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(apiKey);
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> body = new HashMap<>();
			body.put("model", "gpt-4o-mini");

			List<Map<String, String>> messages = new ArrayList<>();

			Map<String, String> systemMsg = new HashMap<>();
			systemMsg.put("role", "system");
			systemMsg.put("content", "You are a legal document analysis assistant.");

			Map<String, String> userMsg = new HashMap<>();
			userMsg.put("role", "user");
			userMsg.put("content", "Context:\n" + context + "\n\nQuestion:\n" + question);

			messages.add(systemMsg);
			messages.add(userMsg);

			body.put("messages", messages);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

			return response.getBody();

		} catch (HttpClientErrorException.TooManyRequests ex) {

			logger.error("OpenAI quota exceeded. Switching to mock response.", ex);
			return buildMockResponse(question);

		} catch (Exception ex) {
			logger.error("OpenAI call failed: ", ex);
			return buildMockResponse(question);
		}

	}

	private String buildMockResponse(String question) {
		return """
				--- PolicyMind AI (Fallback Mode) ---

				Question:
				""" + question + """

				The system is currently operating in fallback mode.
				Please check API quota or billing configuration.
				""";
	}

}
