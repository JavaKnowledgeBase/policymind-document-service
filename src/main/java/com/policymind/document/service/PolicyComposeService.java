package com.policymind.document.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymind.document.dto.PolicyComposeRequest;
import com.policymind.document.exception.DocumentProcessingException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PolicyComposeService {

    private final OpenAiService openAiService;
    private final VertexAiService vertexAiService;
    private final ObjectMapper objectMapper;

    public PolicyComposeService(OpenAiService openAiService,
                                VertexAiService vertexAiService,
                                ObjectMapper objectMapper) {
        this.openAiService = openAiService;
        this.vertexAiService = vertexAiService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> composePolicy(PolicyComposeRequest request) {
        PolicyComposeRequest normalizedRequest = request == null ? new PolicyComposeRequest() : request;
        String mode = normalizeMode(normalizedRequest.getMode());
        String provider = normalizeProvider(normalizedRequest.getProvider());
        validateRequest(mode, normalizedRequest);

        String prompt = buildPrompt(mode, normalizedRequest);
        Map<String, Object> providerOutputs = new HashMap<>();

        if ("openai".equals(provider) || "both".equals(provider)) {
            providerOutputs.put("openai", parseProviderOutput(openAiService.composePolicyDraft(
                    prompt,
                    mode,
                    normalizeText(normalizedRequest.getPolicyType(), "General policy")
            )));
        }

        if ("vertex".equals(provider) || "both".equals(provider)) {
            providerOutputs.put("vertex", parseProviderOutput(vertexAiService.composePolicyDraft(
                    prompt,
                    mode,
                    normalizeText(normalizedRequest.getPolicyType(), "General policy")
            )));
        }

        Map<String, Object> selectedOutput = selectBestOutput(providerOutputs);
        String selectedProvider = resolveSelectedProvider(providerOutputs, selectedOutput);

        Map<String, Object> response = new HashMap<>();
        response.put("mode", mode);
        response.put("provider", provider);
        response.put("selectedProvider", selectedProvider);
        response.put("generatedAt", LocalDateTime.now());
        response.put("policyType", normalizeText(normalizedRequest.getPolicyType(), "General policy"));
        response.put("title", stringValue(selectedOutput.get("title"), normalizeText(normalizedRequest.getTitle(), "Untitled policy draft")));
        response.put("summary", stringValue(selectedOutput.get("summary"), ""));
        response.put("draft", stringValue(selectedOutput.get("draft"), ""));
        response.put("rationale", stringValue(selectedOutput.get("rationale"), ""));
        response.put("confidence", stringValue(selectedOutput.get("confidence"), "medium"));
        response.put("qualityScore", intValue(selectedOutput.get("quality_score"), 70));
        response.put("keyChanges", stringList(selectedOutput.get("key_changes")));
        response.put("implementationChecklist", stringList(selectedOutput.get("implementation_checklist")));
        response.put("riskFlags", stringList(selectedOutput.get("risk_flags")));
        response.put("providers", providerOutputs);
        response.put("structuredOutput", selectedOutput);
        return response;
    }

    private void validateRequest(String mode, PolicyComposeRequest request) {
        if (normalizeText(request.getPolicyType(), "").isBlank()) {
            throw new DocumentProcessingException("Policy type is required.");
        }
        if (normalizeText(request.getGoals(), "").isBlank()) {
            throw new DocumentProcessingException("Goals are required.");
        }
        if ("rewrite".equals(mode) && normalizeText(request.getSourceText(), "").isBlank()) {
            throw new DocumentProcessingException("Source policy text is required for update mode.");
        }
    }

    private String buildPrompt(String mode, PolicyComposeRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Mode: ").append(mode).append("\n");
        prompt.append("Policy type: ").append(normalizeText(request.getPolicyType(), "General policy")).append("\n");
        prompt.append("Title preference: ").append(normalizeText(request.getTitle(), "Create a strong, market-ready title")).append("\n");
        prompt.append("Jurisdiction: ").append(normalizeText(request.getJurisdiction(), "General")).append("\n");
        prompt.append("Audience: ").append(normalizeText(request.getAudience(), "Business users and reviewers")).append("\n");
        prompt.append("Tone: ").append(normalizeText(request.getTone(), "Professional, practical, and clear")).append("\n");
        prompt.append("Business goals:\n").append(normalizeText(request.getGoals(), "Produce a clear, high-quality policy draft")).append("\n");

        List<String> mustInclude = sanitizeList(request.getMustIncludeClauses());
        if (!mustInclude.isEmpty()) {
            prompt.append("Must-include clauses:\n- ").append(String.join("\n- ", mustInclude)).append("\n");
        }

        List<String> prohibited = sanitizeList(request.getProhibitedClauses());
        if (!prohibited.isEmpty()) {
            prompt.append("Avoid or remove:\n- ").append(String.join("\n- ", prohibited)).append("\n");
        }

        String additionalInstructions = normalizeText(request.getAdditionalInstructions(), "");
        if (!additionalInstructions.isBlank()) {
            prompt.append("Additional instructions:\n").append(additionalInstructions).append("\n");
        }

        if ("rewrite".equals(mode)) {
            prompt.append("Source policy text to rewrite:\n").append(normalizeText(request.getSourceText(), "")).append("\n");
            prompt.append("Rewrite goals: preserve intent where reasonable, improve structure, remove ambiguity, tighten governance language, and produce a cleaner publish-ready draft.\n");
        } else {
            prompt.append("Create a new policy from scratch with a complete, publish-ready structure.\n");
        }

        prompt.append("Output expectations: concise executive summary, full polished draft, practical rationale, major changes, rollout checklist, and residual risk flags.");
        return prompt.toString();
    }

    private Map<String, Object> parseProviderOutput(String rawJson) {
        try {
            return objectMapper.readValue(rawJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new DocumentProcessingException("Failed to parse policy compose response.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> selectBestOutput(Map<String, Object> providerOutputs) {
        return providerOutputs.values().stream()
                .filter(Map.class::isInstance)
                .map(value -> (Map<String, Object>) value)
                .max(Comparator.<Map<String, Object>>comparingInt(value -> intValue(value.get("quality_score"), 0))
                        .thenComparingInt(value -> confidenceRank(stringValue(value.get("confidence"), "low"))))
                .orElseThrow(() -> new DocumentProcessingException("No policy composition output was generated."));
    }

    private String resolveSelectedProvider(Map<String, Object> providerOutputs, Map<String, Object> selectedOutput) {
        return providerOutputs.entrySet().stream()
                .filter(entry -> entry.getValue() == selectedOutput)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("openai");
    }

    private int confidenceRank(String confidence) {
        return switch (confidence.toLowerCase()) {
            case "high" -> 3;
            case "medium" -> 2;
            default -> 1;
        };
    }

    private String normalizeMode(String mode) {
        String normalized = normalizeText(mode, "create").toLowerCase();
        if ("rewrite".equals(normalized) || "revise".equals(normalized) || "update".equals(normalized)) {
            return "rewrite";
        }
        return "create";
    }

    private String normalizeProvider(String provider) {
        String normalized = normalizeText(provider, "openai").toLowerCase();
        if ("vertex".equals(normalized) || "both".equals(normalized)) {
            return normalized;
        }
        return "openai";
    }

    private String normalizeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> normalized = new ArrayList<>();
            for (Object entry : list) {
                String text = stringValue(entry, "");
                if (!text.isBlank()) {
                    normalized.add(text);
                }
            }
            return normalized;
        }
        return List.of();
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .toList();
    }
}

