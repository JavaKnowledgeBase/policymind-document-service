package com.policymind.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.policymind.document.config.NetworkClientFactory;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VertexAiService {

    private static final Logger logger = LoggerFactory.getLogger(VertexAiService.class);

    private final String projectId;
    private final String location;
    private final String embeddingModel;
    private final String chatModel;
    private final String configuredBearerToken;
    private final RestTemplate chatRestTemplate;
    private final RestTemplate embeddingRestTemplate;
    private final ObjectMapper objectMapper;
    private final OutboundCallExecutor outboundCallExecutor;

    @Autowired
    public VertexAiService(@Value("${gcp.project-id:}") String projectId,
                           @Value("${gcp.location:us-central1}") String location,
                           @Value("${gcp.vertex.embedding-model:text-embedding-005}") String embeddingModel,
                           @Value("${gcp.vertex.chat-model:gemini-2.0-flash-lite-001}") String chatModel,
                           @Value("${gcp.bearer-token:}") String configuredBearerToken,
                           NetworkClientFactory networkClientFactory,
                           OutboundCallExecutor outboundCallExecutor,
                           ObjectMapper objectMapper) {
        this(projectId,
                location,
                embeddingModel,
                chatModel,
                configuredBearerToken,
                networkClientFactory.createRestTemplate("vertex-chat"),
                networkClientFactory.createRestTemplate("vertex-embedding"),
                outboundCallExecutor,
                objectMapper);
    }

    VertexAiService(String projectId,
                    String location,
                    String embeddingModel,
                    String chatModel,
                    String configuredBearerToken,
                    RestTemplate chatRestTemplate,
                    RestTemplate embeddingRestTemplate,
                    OutboundCallExecutor outboundCallExecutor,
                    ObjectMapper objectMapper) {
        this.projectId = projectId;
        this.location = location;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.configuredBearerToken = configuredBearerToken;
        this.chatRestTemplate = chatRestTemplate;
        this.embeddingRestTemplate = embeddingRestTemplate;
        this.outboundCallExecutor = outboundCallExecutor;
        this.objectMapper = objectMapper;
    }

    public List<Double> generateEmbedding(String text) {
        ensureVertexConfigured();
        try {
            return outboundCallExecutor.execute("vertex-embedding", () -> executeEmbedding(text));
        } catch (Exception ex) {
            logger.error("Vertex embedding call failed", ex);
            throw new IllegalStateException("Vertex embedding service is temporarily unavailable.", ex);
        }
    }

    public String askLLM(String context, String question) {
        ensureVertexConfigured();

        try {
            return outboundCallExecutor.execute("vertex-chat", () -> executeChatCompletion(context, question));
        } catch (CallNotPermittedException | BulkheadFullException | OutboundCallTimeoutException ex) {
            logger.warn("Vertex AI resilience policy triggered. Switching to fallback.", ex);
            return buildFallbackResponse(question, "vertex_temporarily_unavailable");
        } catch (Exception ex) {
            logger.error("Vertex AI call failed", ex);
            return buildFallbackResponse(question, "vertex_llm_failed");
        }
    }

    public String composePolicyDraft(String prompt, String mode, String policyType) {
        ensureVertexConfigured();

        try {
            return outboundCallExecutor.execute("vertex-chat", () -> executePolicyCompose(prompt, mode, policyType));
        } catch (CallNotPermittedException | BulkheadFullException | OutboundCallTimeoutException ex) {
            logger.warn("Vertex AI resilience policy triggered during policy compose. Switching to fallback.", ex);
            return buildPolicyComposeFallback(mode, policyType, "vertex_temporarily_unavailable");
        } catch (Exception ex) {
            logger.error("Vertex AI policy compose call failed", ex);
            return buildPolicyComposeFallback(mode, policyType, "vertex_llm_failed");
        }
    }

    private List<Double> executeEmbedding(String text) {
        String url = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict",
                location, projectId, location, embeddingModel
        );

        Map<String, Object> body = new HashMap<>();
        body.put("instances", List.of(Map.of("content", text)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
        ResponseEntity<String> response = embeddingRestTemplate.postForEntity(url, request, String.class);
        return extractEmbedding(response.getBody());
    }

    private String executeChatCompletion(String context, String question) {
        String prompt =
                "You are a legal and policy analysis assistant. Return STRICT JSON with keys: " +
                        "summary, answer, risk_score, key_risks, recommended_actions, confidence.\n" +
                        "Context:\n" + context + "\nQuestion:\n" + question +
                        "\nReturn JSON only. risk_score must be integer 1-10. confidence must be low|medium|high. " +
                        "key_risks and recommended_actions must be arrays of strings.";

        String content = generateContent(prompt);
        if (content == null || content.isBlank()) {
            return buildFallbackResponse(question, "vertex_empty_content");
        }

        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(stripMarkdownCodeFence(content));
        } catch (Exception ex) {
            logger.warn("Failed to parse Vertex structured output. Returning fallback JSON.", ex);
            return buildFallbackResponse(question, "vertex_unstructured_output");
        }

        Map<String, Object> output = new HashMap<>();
        output.put("summary", parsed.path("summary").asText("Summary not provided."));
        output.put("answer", parsed.path("answer").asText("Answer not provided."));
        output.put("risk_score", parsed.path("risk_score").asInt(5));
        output.put("confidence", parsed.path("confidence").asText("medium"));
        output.put("key_risks", toList(parsed.path("key_risks")));
        output.put("recommended_actions", toList(parsed.path("recommended_actions")));
        try {
            return objectMapper.writeValueAsString(output);
        } catch (Exception ex) {
            return buildFallbackResponse(question, "vertex_output_serialization_failed");
        }
    }

    private String executePolicyCompose(String prompt, String mode, String policyType) {
        String content = generateContent(
                "You are a world-class enterprise policy drafting assistant. Return STRICT JSON with keys: title, summary, draft, rationale, key_changes, implementation_checklist, risk_flags, confidence, quality_score. The draft must be polished, practical, and publish-ready.\n" +
                        prompt +
                        "\nReturn JSON only. quality_score must be integer 1-100. confidence must be low|medium|high. key_changes, implementation_checklist, and risk_flags must be arrays of strings."
        );

        if (content == null || content.isBlank()) {
            return buildPolicyComposeFallback(mode, policyType, "vertex_empty_content");
        }

        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(stripMarkdownCodeFence(content));
        } catch (Exception ex) {
            logger.warn("Failed to parse Vertex policy compose output. Returning fallback JSON.", ex);
            return buildPolicyComposeFallback(mode, policyType, "vertex_unstructured_output");
        }

        Map<String, Object> output = new HashMap<>();
        output.put("title", fallbackText(parsed.path("title").asText(""), policyType + ("rewrite".equals(mode) ? " Update Draft" : " Draft")));
        output.put("summary", fallbackText(parsed.path("summary").asText(""), "Draft summary not provided."));
        output.put("draft", fallbackText(parsed.path("draft").asText(""), "Draft text not provided."));
        output.put("rationale", fallbackText(parsed.path("rationale").asText(""), "Rationale not provided."));
        output.put("key_changes", toList(parsed.path("key_changes")));
        output.put("implementation_checklist", toList(parsed.path("implementation_checklist")));
        output.put("risk_flags", toList(parsed.path("risk_flags")));
        output.put("confidence", parsed.path("confidence").asText("medium"));
        output.put("quality_score", parsed.path("quality_score").asInt(76));
        try {
            return objectMapper.writeValueAsString(output);
        } catch (Exception ex) {
            return buildPolicyComposeFallback(mode, policyType, "vertex_output_serialization_failed");
        }
    }

    private String generateContent(String prompt) {
        String url = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                location, projectId, location, chatModel
        );

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))));
        body.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
        ResponseEntity<String> response = chatRestTemplate.postForEntity(url, request, String.class);
        return extractGeneratedText(response.getBody());
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resolveAccessToken());
        return headers;
    }

    private String resolveAccessToken() {
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
            logger.error("Failed to obtain access token via ADC.", e);
        }

        String envToken = System.getenv("GCP_BEARER_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken.trim();
        }

        throw new IllegalStateException(
                "Vertex access token not found. Configure GOOGLE_APPLICATION_CREDENTIALS " +
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

    private List<?> toList(JsonNode node) {
        return node.isArray() ? objectMapper.convertValue(node, List.class) : List.of();
    }

    private String fallbackText(String value, String fallback) {
        if (value == null || value.trim().isBlank()) {
            return fallback;
        }
        return value.trim();
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

    private String buildPolicyComposeFallback(String mode, String policyType, String reason) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("title", policyType + ("rewrite".equals(mode) ? " Update Draft" : " Draft"));
        fallback.put("summary", "Vertex policy compose fallback response");
        fallback.put("draft", "The drafting service is temporarily unavailable. Retry once connectivity and provider capacity recover.");
        fallback.put("rationale", "A full policy draft could not be produced right now, so this fallback preserves the workflow while signaling low confidence.");
        fallback.put("key_changes", List.of("AI drafting temporarily unavailable"));
        fallback.put("implementation_checklist", List.of("Retry generation", "Review Vertex config and logs"));
        fallback.put("risk_flags", List.of("Draft incomplete until regenerated"));
        fallback.put("confidence", "low");
        fallback.put("quality_score", 44);
        fallback.put("fallback_reason", reason);

        try {
            return objectMapper.writeValueAsString(fallback);
        } catch (Exception e) {
            return "{\"title\":\"Policy Draft\",\"summary\":\"Vertex policy compose fallback response\",\"draft\":\"Service temporarily unavailable\",\"confidence\":\"low\",\"quality_score\":44}";
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
