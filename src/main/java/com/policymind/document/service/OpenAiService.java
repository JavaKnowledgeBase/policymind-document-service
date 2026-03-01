package com.policymind.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String askLLM(String context, String question) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-4o-mini");
            body.put("temperature", 0.2);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put(
                    "content",
                    "You are a legal and policy analysis assistant. Return STRICT JSON with keys: summary, answer, risk_score, key_risks, recommended_actions, confidence."
            );

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put(
                    "content",
                    "Context:\n" + context +
                            "\nQuestion:\n" + question +
                            "\nReturn JSON only. risk_score must be integer 1-10. confidence must be low|medium|high. key_risks and recommended_actions must be arrays of strings."
            );

            messages.add(systemMsg);
            messages.add(userMsg);
            body.put("messages", messages);

            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            body.put("response_format", responseFormat);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            return normalizeStructuredOutput(response.getBody(), question);

        } catch (HttpClientErrorException.TooManyRequests ex) {
            logger.error("OpenAI quota exceeded. Switching to fallback.", ex);
            return buildFallbackResponse(question, "quota_exceeded");
        } catch (Exception ex) {
            logger.error("OpenAI call failed", ex);
            return buildFallbackResponse(question, "llm_call_failed");
        }
    }

    private String normalizeStructuredOutput(String openAiApiJson, String question) {
        try {
            JsonNode root = objectMapper.readTree(openAiApiJson);
            String content = root.path("choices").path(0).path("message").path("content").asText("");

            if (content.isBlank()) {
                return buildFallbackResponse(question, "empty_llm_content");
            }

            JsonNode parsed = objectMapper.readTree(content);

            Map<String, Object> output = new HashMap<>();
            output.put("summary", parsed.path("summary").asText("Summary not provided."));
            output.put("answer", parsed.path("answer").asText("Answer not provided."));
            output.put("risk_score", parsed.path("risk_score").asInt(5));
            output.put("confidence", parsed.path("confidence").asText("medium"));

            output.put(
                    "key_risks",
                    parsed.path("key_risks").isArray()
                            ? objectMapper.convertValue(parsed.path("key_risks"), List.class)
                            : List.of()
            );

            output.put(
                    "recommended_actions",
                    parsed.path("recommended_actions").isArray()
                            ? objectMapper.convertValue(parsed.path("recommended_actions"), List.class)
                            : List.of()
            );

            return objectMapper.writeValueAsString(output);
        } catch (Exception e) {
            logger.warn("Failed to parse structured LLM output. Returning fallback JSON.");
            return buildFallbackResponse(question, "unstructured_llm_output");
        }
    }

    private String buildFallbackResponse(String question, String reason) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("summary", "PolicyMind fallback response");
        fallback.put("answer", "Could not generate full AI response for the provided question right now.");
        fallback.put("risk_score", 5);
        fallback.put("confidence", "low");
        fallback.put("key_risks", List.of("LLM response unavailable", "Please retry later"));
        fallback.put("recommended_actions", List.of("Retry the same query", "Check API key/quota and service logs"));
        fallback.put("question", question);
        fallback.put("fallback_reason", reason);

        try {
            return objectMapper.writeValueAsString(fallback);
        } catch (Exception e) {
            return "{\"summary\":\"PolicyMind fallback response\",\"answer\":\"Service temporarily unavailable\",\"risk_score\":5,\"confidence\":\"low\"}";
        }
    }
}
