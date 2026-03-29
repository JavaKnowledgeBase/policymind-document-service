package com.policymind.document.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymind.document.dto.PolicyDraftSaveRequest;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.model.PolicyDraft;
import com.policymind.document.model.PolicyDraftVersion;
import com.policymind.document.repository.PolicyDraftRepository;
import com.policymind.document.repository.PolicyDraftVersionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PolicyDraftService {

    private final PolicyDraftRepository policyDraftRepository;
    private final PolicyDraftVersionRepository policyDraftVersionRepository;
    private final ObjectMapper objectMapper;

    public PolicyDraftService(PolicyDraftRepository policyDraftRepository,
                              PolicyDraftVersionRepository policyDraftVersionRepository,
                              ObjectMapper objectMapper) {
        this.policyDraftRepository = policyDraftRepository;
        this.policyDraftVersionRepository = policyDraftVersionRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> saveDraft(PolicyDraftSaveRequest request) {
        PolicyDraftSaveRequest normalizedRequest = request == null ? new PolicyDraftSaveRequest() : request;
        validateRequest(normalizedRequest);

        PolicyDraft draft = normalizedRequest.getDraftId() == null
                ? new PolicyDraft()
                : policyDraftRepository.findById(normalizedRequest.getDraftId())
                .orElseThrow(() -> new DocumentProcessingException("Policy draft not found: " + normalizedRequest.getDraftId()));

        LocalDateTime now = LocalDateTime.now();
        boolean isNewDraft = draft.getId() == null;
        Integer nextVersion = (draft.getCurrentVersionNumber() == null ? 0 : draft.getCurrentVersionNumber()) + 1;

        draft.setMode(normalizeText(normalizedRequest.getMode(), "create"));
        draft.setProvider(normalizeText(normalizedRequest.getProvider(), "openai"));
        draft.setPolicyType(normalizeText(normalizedRequest.getPolicyType(), "General policy"));
        draft.setTitle(normalizeText(normalizedRequest.getTitle(), draft.getPolicyType() + " Draft"));
        draft.setJurisdiction(normalizeText(normalizedRequest.getJurisdiction(), "United States"));
        draft.setAudience(normalizeText(normalizedRequest.getAudience(), "Employees and managers"));
        draft.setTone(normalizeText(normalizedRequest.getTone(), "Clear, practical, and professional"));
        draft.setGoals(normalizeText(normalizedRequest.getGoals(), ""));
        draft.setAdditionalInstructions(normalizeText(normalizedRequest.getAdditionalInstructions(), ""));
        draft.setSourceText(normalizeText(normalizedRequest.getSourceText(), ""));
        draft.setWorkingDraft(normalizeText(normalizedRequest.getWorkingDraft(), ""));
        draft.setLatestSummary(normalizeText(normalizedRequest.getSummary(), ""));
        draft.setLatestRationale(normalizeText(normalizedRequest.getRationale(), ""));
        draft.setLatestConfidence(normalizeText(normalizedRequest.getConfidence(), "medium"));
        draft.setLatestQualityScore(normalizedRequest.getQualityScore() == null ? 0 : normalizedRequest.getQualityScore());
        draft.setCurrentVersionNumber(nextVersion);
        draft.setUpdatedAt(now);
        draft.setLastGeneratedAt(now);
        if (isNewDraft) {
            draft.setCreatedAt(now);
        }
        draft = policyDraftRepository.save(draft);

        PolicyDraftVersion version = new PolicyDraftVersion();
        version.setDraft(draft);
        version.setVersionNumber(nextVersion);
        version.setQualityScore(draft.getLatestQualityScore());
        version.setConfidence(draft.getLatestConfidence());
        version.setCreatedAt(now);
        version.setSummary(draft.getLatestSummary());
        version.setRationale(draft.getLatestRationale());
        version.setSourceText(draft.getSourceText());
        version.setWorkingDraft(draft.getWorkingDraft());
        version.setMustIncludeClausesJson(writeJson(normalizedRequest.getMustIncludeClauses()));
        version.setProhibitedClausesJson(writeJson(normalizedRequest.getProhibitedClauses()));
        version.setKeyChangesJson(writeJson(normalizedRequest.getKeyChanges()));
        version.setImplementationChecklistJson(writeJson(normalizedRequest.getImplementationChecklist()));
        version.setRiskFlagsJson(writeJson(normalizedRequest.getRiskFlags()));
        version.setComposeResultJson(writeJson(normalizedRequest.getComposeResult()));
        version = policyDraftVersionRepository.save(version);

        return toDraftDetailMap(draft, version);
    }

    public List<Map<String, Object>> listDrafts() {
        return policyDraftRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toDraftListItem)
                .toList();
    }

    public Map<String, Object> getDraft(Long draftId) {
        PolicyDraft draft = policyDraftRepository.findById(draftId)
                .orElseThrow(() -> new DocumentProcessingException("Policy draft not found: " + draftId));
        List<PolicyDraftVersion> versions = policyDraftVersionRepository.findByDraft_IdOrderByVersionNumberDesc(draftId);
        PolicyDraftVersion latestVersion = versions.isEmpty() ? null : versions.get(0);
        return toDraftDetailMap(draft, latestVersion);
    }

    public List<Map<String, Object>> listDraftVersions(Long draftId) {
        policyDraftRepository.findById(draftId)
                .orElseThrow(() -> new DocumentProcessingException("Policy draft not found: " + draftId));
        return policyDraftVersionRepository.findByDraft_IdOrderByVersionNumberDesc(draftId).stream()
                .map(this::toVersionMap)
                .toList();
    }

    private void validateRequest(PolicyDraftSaveRequest request) {
        if (normalizeText(request.getPolicyType(), "").isBlank()) {
            throw new DocumentProcessingException("Policy type is required to save a draft.");
        }
        if (normalizeText(request.getWorkingDraft(), "").isBlank()) {
            throw new DocumentProcessingException("Working draft text is required to save a draft.");
        }
    }

    private Map<String, Object> toDraftListItem(PolicyDraft draft) {
        Map<String, Object> response = new HashMap<>();
        response.put("draftId", draft.getId());
        response.put("title", draft.getTitle());
        response.put("policyType", draft.getPolicyType());
        response.put("mode", draft.getMode());
        response.put("provider", draft.getProvider());
        response.put("currentVersionNumber", draft.getCurrentVersionNumber());
        response.put("latestQualityScore", draft.getLatestQualityScore());
        response.put("latestConfidence", draft.getLatestConfidence());
        response.put("updatedAt", draft.getUpdatedAt());
        response.put("graphWorkflow", buildGraphWorkflow(draft.getMode(), draft.getCurrentVersionNumber(), true));
        return response;
    }

    private Map<String, Object> toDraftDetailMap(PolicyDraft draft, PolicyDraftVersion latestVersion) {
        Map<String, Object> response = new HashMap<>();
        response.put("draftId", draft.getId());
        response.put("title", draft.getTitle());
        response.put("policyType", draft.getPolicyType());
        response.put("mode", draft.getMode());
        response.put("provider", draft.getProvider());
        response.put("jurisdiction", draft.getJurisdiction());
        response.put("audience", draft.getAudience());
        response.put("tone", draft.getTone());
        response.put("goals", draft.getGoals());
        response.put("additionalInstructions", draft.getAdditionalInstructions());
        response.put("sourceText", draft.getSourceText());
        response.put("workingDraft", draft.getWorkingDraft());
        response.put("summary", draft.getLatestSummary());
        response.put("rationale", draft.getLatestRationale());
        response.put("confidence", draft.getLatestConfidence());
        response.put("qualityScore", draft.getLatestQualityScore());
        response.put("currentVersionNumber", draft.getCurrentVersionNumber());
        response.put("createdAt", draft.getCreatedAt());
        response.put("updatedAt", draft.getUpdatedAt());
        response.put("lastGeneratedAt", draft.getLastGeneratedAt());
        response.put("mustIncludeClauses", latestVersion == null ? List.of() : readJson(latestVersion.getMustIncludeClausesJson()));
        response.put("prohibitedClauses", latestVersion == null ? List.of() : readJson(latestVersion.getProhibitedClausesJson()));
        response.put("keyChanges", latestVersion == null ? List.of() : readJson(latestVersion.getKeyChangesJson()));
        response.put("implementationChecklist", latestVersion == null ? List.of() : readJson(latestVersion.getImplementationChecklistJson()));
        response.put("riskFlags", latestVersion == null ? List.of() : readJson(latestVersion.getRiskFlagsJson()));
        response.put("composeResult", latestVersion == null ? Map.of() : readJson(latestVersion.getComposeResultJson()));
        response.put("graphWorkflow", buildGraphWorkflow(draft.getMode(), draft.getCurrentVersionNumber(), draft.getLastGeneratedAt() != null));
        return response;
    }

    private Map<String, Object> toVersionMap(PolicyDraftVersion version) {
        Map<String, Object> response = new HashMap<>();
        response.put("versionId", version.getId());
        response.put("draftId", version.getDraft() == null ? null : version.getDraft().getId());
        response.put("versionNumber", version.getVersionNumber());
        response.put("qualityScore", version.getQualityScore());
        response.put("confidence", version.getConfidence());
        response.put("summary", version.getSummary());
        response.put("rationale", version.getRationale());
        response.put("sourceText", version.getSourceText());
        response.put("workingDraft", version.getWorkingDraft());
        response.put("mustIncludeClauses", readJson(version.getMustIncludeClausesJson()));
        response.put("prohibitedClauses", readJson(version.getProhibitedClausesJson()));
        response.put("keyChanges", readJson(version.getKeyChangesJson()));
        response.put("implementationChecklist", readJson(version.getImplementationChecklistJson()));
        response.put("riskFlags", readJson(version.getRiskFlagsJson()));
        response.put("composeResult", readJson(version.getComposeResultJson()));
        response.put("createdAt", version.getCreatedAt());
        response.put("graphWorkflow", buildGraphWorkflow(version.getDraft() == null ? "create" : version.getDraft().getMode(), version.getVersionNumber(), true));
        return response;
    }

    private Map<String, Object> buildGraphWorkflow(String mode, Integer versionNumber, boolean generated) {
        List<Map<String, Object>> nodes = List.of(
                workflowNode("intake", "completed"),
                workflowNode("mode_selection", "completed"),
                workflowNode("workspace_creation", normalizeText(mode, "create").equals("rewrite") ? "completed" : "completed"),
                workflowNode("generate_or_rewrite", generated ? "completed" : "ready"),
                workflowNode("review_insights", generated ? "completed" : "ready"),
                workflowNode("save_draft", versionNumber != null && versionNumber > 0 ? "completed" : "ready"),
                workflowNode("version_history", versionNumber != null && versionNumber > 1 ? "completed" : "ready")
        );
        return Map.of(
                "versionNumber", versionNumber == null ? 0 : versionNumber,
                "nodes", nodes
        );
    }

    private Map<String, Object> workflowNode(String key, String status) {
        return Map.of("key", key, "status", status);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            throw new DocumentProcessingException("Failed to serialize policy draft data.", ex);
        }
    }

    private Object readJson(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return value;
        }
    }

    private String normalizeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? fallback : normalized;
    }
}
