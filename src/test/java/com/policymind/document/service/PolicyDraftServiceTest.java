package com.policymind.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymind.document.dto.PolicyDraftSaveRequest;
import com.policymind.document.model.PolicyDraft;
import com.policymind.document.model.PolicyDraftVersion;
import com.policymind.document.repository.PolicyDraftRepository;
import com.policymind.document.repository.PolicyDraftVersionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyDraftServiceTest {

    @Test
    public void saveDraft_createsDraftAndVersion() {
        PolicyDraftRepository draftRepository = mock(PolicyDraftRepository.class);
        PolicyDraftVersionRepository versionRepository = mock(PolicyDraftVersionRepository.class);
        PolicyDraftService service = new PolicyDraftService(draftRepository, versionRepository, new ObjectMapper());

        when(draftRepository.save(any(PolicyDraft.class))).thenAnswer(invocation -> {
            PolicyDraft draft = invocation.getArgument(0);
            if (draft.getId() == null) {
                draft.setId(10L);
            }
            return draft;
        });
        when(versionRepository.save(any(PolicyDraftVersion.class))).thenAnswer(invocation -> {
            PolicyDraftVersion version = invocation.getArgument(0);
            version.setId(20L);
            return version;
        });

        PolicyDraftSaveRequest request = new PolicyDraftSaveRequest();
        request.setMode("rewrite");
        request.setProvider("openai");
        request.setPolicyType("Security Policy");
        request.setTitle("Security Policy Draft");
        request.setWorkingDraft("draft text");
        request.setSourceText("source text");
        request.setSummary("summary");
        request.setRationale("rationale");
        request.setQualityScore(88);
        request.setConfidence("high");
        request.setMustIncludeClauses(List.of("Add approval workflow"));
        request.setProhibitedClauses(List.of("Avoid vague discretion"));
        request.setKeyChanges(List.of("Clarified scope"));
        request.setImplementationChecklist(List.of("Legal review"));
        request.setRiskFlags(List.of("Pending approval"));
        request.setComposeResult(Map.of("draft", "draft text"));

        Map<String, Object> response = service.saveDraft(request);

        assertEquals(10L, response.get("draftId"));
        assertEquals(1, response.get("currentVersionNumber"));
        assertTrue(response.get("mustIncludeClauses") instanceof List);
        assertTrue(response.get("prohibitedClauses") instanceof List);
        assertTrue(response.get("graphWorkflow") instanceof Map);
    }

    @Test
    public void getDraft_returnsLatestVersionData() {
        PolicyDraftRepository draftRepository = mock(PolicyDraftRepository.class);
        PolicyDraftVersionRepository versionRepository = mock(PolicyDraftVersionRepository.class);
        PolicyDraftService service = new PolicyDraftService(draftRepository, versionRepository, new ObjectMapper());

        PolicyDraft draft = new PolicyDraft();
        draft.setId(10L);
        draft.setTitle("Security Policy Draft");
        draft.setPolicyType("Security Policy");
        draft.setMode("rewrite");
        draft.setProvider("openai");
        draft.setCurrentVersionNumber(2);

        PolicyDraftVersion version = new PolicyDraftVersion();
        version.setId(20L);
        version.setDraft(draft);
        version.setVersionNumber(2);
        version.setKeyChangesJson("[\"Clarified scope\"]");
        version.setImplementationChecklistJson("[\"Legal review\"]");
        version.setRiskFlagsJson("[\"Pending approval\"]");
        version.setComposeResultJson("{\"draft\":\"draft text\"}");

        when(draftRepository.findById(10L)).thenReturn(Optional.of(draft));
        when(versionRepository.findByDraft_IdOrderByVersionNumberDesc(10L)).thenReturn(List.of(version));

        Map<String, Object> response = service.getDraft(10L);

        assertEquals(10L, response.get("draftId"));
        assertTrue(response.get("mustIncludeClauses") instanceof List);
        assertTrue(response.get("prohibitedClauses") instanceof List);
        assertTrue(response.get("keyChanges") instanceof List);
        assertTrue(response.get("composeResult") instanceof Map);
    }
}

