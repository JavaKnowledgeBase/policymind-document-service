package com.policymind.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VertexAiService {

    private static final Logger logger = LoggerFactory.getLogger(VertexAiService.class);

    @Value("${gcp.project-id:}")
    private String projectId;

    @Value("${gcp.location:us-central1}")
    private String location;

    @Value("${gcp.vertex.embedding-model:text-embedding-005}")
    private String embeddingModel;

    @Value("${gcp.vertex.chat-model:gemini-2.0-flash-lite-001}")
    private String chatModel;

    @Value("${gcp.bearer-token:}")
    private String configuredBearerToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Double> generateEmbedding(String text) {
        ensureVertexConfigured();

        String url = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict",
                location, projectId, location, embeddingModel
        );

        Map<String, Object> body = new HashMap<>();
        body.put("instances", List.of(Map.of("content", text)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return extractEmbedding(response.getBody());
    }

    public String askLLM(String context, String question) {
        ensureVertexConfigured();

        try {
            String url = String.format(
                    "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                    location, projectId, location, chatModel
            );

            String prompt =
                    "You are a legal and policy analysis assistant. Return STRICT JSON with keys: " +
                            "summary, answer, risk_score, key_risks, recommended_actions, confidence.\n" +
                            "Context:\n" + context + "\nQuestion:\n" + question +
                            "\nReturn JSON only. risk_score must be integer 1-10. confidence must be low|medium|high. " +
                            "key_risks and recommended_actions must be arrays of strings.";

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.2);

            Map<String, Object> body = new HashMap<>();
            body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))));
            body.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            String content = extractGeneratedText(response.getBody());
            if (content == null || content.isBlank()) {
                return buildFallbackResponse(question, "vertex_empty_content");
            }

            JsonNode parsed = objectMapper.readTree(stripMarkdownCodeFence(content));
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
        } catch (Exception ex) {
            logger.error("Vertex AI call failed", ex);
            return buildFallbackResponse(question, "vertex_llm_failed");
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resolveAccessToken());
        return headers;
    }

    private String resolveAccessToken() {
        // Optional manual override (keep if you want emergency fallback)
        if (configuredBearerToken != null && !configuredBearerToken.isBlank()) {
            return configuredBearerToken.trim();
        }

        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            if (token != null && token.getTokenValue() != null && !token.getTokenValue().isBlank()) {
                return token.getTokenValue();
            }
        } catch (Exception e) {
            logger.error("Failed to obtain ADC token for Vertex AI.", e);
        }

        throw new IllegalStateException(
                "Vertex access token not found via ADC. Configure GOOGLE_APPLICATION_CREDENTIALS " +
                "or run on GCP with Workload Identity."
        );
    }


    private List<Double> extractEmbedding(String apiJson) {
        try {
            JsonNode root = objectMapper.readTree(apiJson);
            JsonNode prediction = root.path("predictions").path(0);
            JsonNode vectorNode = prediction.path("embeddings").path("values");
            if (!vectorNode.isArray()) {
                vectorNode = prediction.path("values");
            }

            List<Double> vector = new ArrayList<>();
            for (JsonNode value : vectorNode) {
                vector.add(value.asDouble());
            }

            if (vector.isEmpty()) {
                throw new IllegalStateException("Vertex embedding vector is empty.");
            }
            return vector;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Vertex embedding response", e);
        }
    }

    private String extractGeneratedText(String apiJson) {
        try {
            JsonNode root = objectMapper.readTree(apiJson);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        } catch (Exception e) {
            logger.warn("Failed to parse Vertex generateContent response");
            return "";
        }
    }

    private String buildFallbackResponse(String question, String reason) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("summary", "Vertex fallback response");
        fallback.put("answer", "Could not generate full Vertex response for the provided question right now.");
        fallback.put("risk_score", 5);
        fallback.put("confidence", "low");
        fallback.put("key_risks", List.of("Vertex response unavailable", "Please retry later"));
        fallback.put("recommended_actions", List.of("Retry the same query", "Check Vertex config and service logs"));
        fallback.put("question", question);
        fallback.put("fallback_reason", reason);

        try {
            return objectMapper.writeValueAsString(fallback);
        } catch (Exception e) {
            return "{\"summary\":\"Vertex fallback response\",\"answer\":\"Service temporarily unavailable\",\"risk_score\":5,\"confidence\":\"low\"}";
        }
    }

    private void ensureVertexConfigured() {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("gcp.project-id is required for Vertex AI.");
        }
    }

    private String stripMarkdownCodeFence(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        String withoutStart = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return withoutStart.replaceFirst("\\s*```$", "").trim();
    }
    
}
