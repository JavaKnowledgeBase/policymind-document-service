package com.policymind.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OutboundCallExecutor outboundCallExecutor;

    @Autowired
    public OpenAiService(@Value("${openai.api.key}") String apiKey,
                         NetworkClientFactory networkClientFactory,
                         OutboundCallExecutor outboundCallExecutor,
                         ObjectMapper objectMapper) {
        this(apiKey, networkClientFactory.createRestTemplate("openai-chat"), outboundCallExecutor, objectMapper);
    }

    OpenAiService(String apiKey,
                  RestTemplate restTemplate,
                  OutboundCallExecutor outboundCallExecutor,
                  ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.outboundCallExecutor = outboundCallExecutor;
        this.objectMapper = objectMapper;
    }

    public String askLLM(String context, String question) {
        try {
            return outboundCallExecutor.execute("openai-chat", () -> executeChatCompletion(context, question));
        } catch (HttpClientErrorException.TooManyRequests ex) {
            logger.error("OpenAI quota exceeded. Switching to fallback.", ex);
            return buildFallbackResponse(question, "quota_exceeded");
        } catch (CallNotPermittedException | BulkheadFullException | OutboundCallTimeoutException ex) {
            logger.warn("OpenAI resilience policy triggered. Switching to fallback.", ex);
            return buildFallbackResponse(question, "openai_temporarily_unavailable");
        } catch (Exception ex) {
            logger.error("OpenAI call failed", ex);
            return buildFallbackResponse(question, "llm_call_failed");
        }
    }

    public String composePolicyDraft(String prompt, String mode, String policyType) {
        try {
            return outboundCallExecutor.execute("openai-chat", () -> executePolicyCompose(prompt, mode, policyType));
        } catch (HttpClientErrorException.TooManyRequests ex) {
            logger.error("OpenAI quota exceeded during policy compose. Switching to fallback.", ex);
            return buildPolicyComposeFallback(mode, policyType, "quota_exceeded");
        } catch (CallNotPermittedException | BulkheadFullException | OutboundCallTimeoutException ex) {
            logger.warn("OpenAI resilience policy triggered during policy compose. Switching to fallback.", ex);
            return buildPolicyComposeFallback(mode, policyType, "openai_temporarily_unavailable");
        } catch (Exception ex) {
            logger.error("OpenAI policy compose call failed", ex);
            return buildPolicyComposeFallback(mode, policyType, "llm_call_failed");
        }
    }

    private String executeChatCompletion(String context, String question) {
        String systemPrompt = "You are a legal and policy analysis assistant. Return STRICT JSON with keys: summary, answer, risk_score, key_risks, recommended_actions, confidence.";
        String userPrompt = "Context:\n" + context +
                "\nQuestion:\n" + question +
                "\nReturn JSON only. risk_score must be integer 1-10. confidence must be low|medium|high. key_risks and recommended_actions must be arrays of strings.";

        return executeStructuredChat(systemPrompt, userPrompt, content -> normalizeStructuredOutput(content, question));
    }

    private String executePolicyCompose(String prompt, String mode, String policyType) {
        String systemPrompt = "You are a world-class enterprise policy drafting assistant. Return STRICT JSON with keys: title, summary, draft, rationale, key_changes, implementation_checklist, risk_flags, confidence, quality_score. The draft must be polished, practical, and publish-ready.";
        String userPrompt = prompt + "\nReturn JSON only. quality_score must be integer 1-100. confidence must be low|medium|high. key_changes, implementation_checklist, and risk_flags must be arrays of strings.";

        return executeStructuredChat(systemPrompt, userPrompt, content -> normalizePolicyComposeOutput(content, mode, policyType));
    }

    private String executeStructuredChat(String systemPrompt, String userPrompt, StructuredResponseNormalizer normalizer) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("temperature", 0.2);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);
        body.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return normalizer.normalize(response.getBody());
    }

    private String normalizeStructuredOutput(String openAiApiJson, String question) {
        try {
            JsonNode parsed = extractContentJson(openAiApiJson);

            String summary = parsed.path("summary").asText("").trim();
            if (summary.isBlank()) {
                summary = "Summary not provided.";
            }

            String answer = parsed.path("answer").asText("").trim();
            if (answer.isBlank()) {
                answer = summary;
            }

            Map<String, Object> output = new HashMap<>();
            output.put("summary", summary);
            output.put("answer", answer);
            output.put("risk_score", parsed.path("risk_score").asInt(5));
            output.put("confidence", parsed.path("confidence").asText("medium"));
            output.put("key_risks", jsonArrayToList(parsed.path("key_risks")));
            output.put("recommended_actions", jsonArrayToList(parsed.path("recommended_actions")));
            return objectMapper.writeValueAsString(output);
        } catch (Exception e) {
            logger.warn("Failed to parse structured LLM output. Returning fallback JSON.");
            return buildFallbackResponse(question, "unstructured_llm_output");
        }
    }

    private String normalizePolicyComposeOutput(String openAiApiJson, String mode, String policyType) {
        try {
            JsonNode parsed = extractContentJson(openAiApiJson);

            Map<String, Object> output = new HashMap<>();
            output.put("title", fallbackText(parsed.path("title").asText(""), policyType + ("rewrite".equals(mode) ? " Update Draft" : " Draft")));
            output.put("summary", fallbackText(parsed.path("summary").asText(""), "Draft summary not provided."));
            output.put("draft", fallbackText(parsed.path("draft").asText(""), "Draft text not provided."));
            output.put("rationale", fallbackText(parsed.path("rationale").asText(""), "Rationale not provided."));
            output.put("key_changes", jsonArrayToList(parsed.path("key_changes")));
            output.put("implementation_checklist", jsonArrayToList(parsed.path("implementation_checklist")));
            output.put("risk_flags", jsonArrayToList(parsed.path("risk_flags")));
            output.put("confidence", parsed.path("confidence").asText("medium"));
            output.put("quality_score", parsed.path("quality_score").asInt(78));
            return objectMapper.writeValueAsString(output);
        } catch (Exception e) {
            logger.warn("Failed to parse policy compose output. Returning fallback JSON.");
            return buildPolicyComposeFallback(mode, policyType, "unstructured_llm_output");
        }
    }

    private JsonNode extractContentJson(String apiJson) throws Exception {
        JsonNode root = objectMapper.readTree(apiJson);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("empty_llm_content");
        }
        return objectMapper.readTree(content);
    }

    private List<?> jsonArrayToList(JsonNode node) {
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

    private String buildPolicyComposeFallback(String mode, String policyType, String reason) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("title", policyType + ("rewrite".equals(mode) ? " Update Draft" : " Draft"));
        fallback.put("summary", "Policy compose fallback response");
        fallback.put("draft", "The drafting service is temporarily unavailable. Retry once connectivity and provider capacity recover.");
        fallback.put("rationale", "A full policy draft could not be produced right now, so this fallback preserves the workflow while signaling low confidence.");
        fallback.put("key_changes", List.of("AI drafting temporarily unavailable"));
        fallback.put("implementation_checklist", List.of("Retry generation", "Review provider quota and logs"));
        fallback.put("risk_flags", List.of("Draft incomplete until regenerated"));
        fallback.put("confidence", "low");
        fallback.put("quality_score", 45);
        fallback.put("fallback_reason", reason);

        try {
            return objectMapper.writeValueAsString(fallback);
        } catch (Exception e) {
            return "{\"title\":\"Policy Draft\",\"summary\":\"Policy compose fallback response\",\"draft\":\"Service temporarily unavailable\",\"confidence\":\"low\",\"quality_score\":45}";
        }
    }

    @FunctionalInterface
    private interface StructuredResponseNormalizer {
        String normalize(String apiResponse);
    }
}
