package com.policymind.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymind.document.dto.PolicyComposeRequest;
import com.policymind.document.exception.DocumentProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyComposeServiceTest {

    @Test
    public void composePolicy_rewriteMode_returnsBestProviderOutput() {
        OpenAiService openAiService = mock(OpenAiService.class);
        VertexAiService vertexAiService = mock(VertexAiService.class);
        PolicyComposeService service = new PolicyComposeService(openAiService, vertexAiService, new ObjectMapper());

        when(openAiService.composePolicyDraft(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("rewrite"), org.mockito.ArgumentMatchers.eq("Security Policy")))
                .thenReturn("{\"title\":\"Security Policy Rewrite\",\"summary\":\"Improved clarity\",\"draft\":\"Full draft text\",\"rationale\":\"Aligned terms\",\"key_changes\":[\"Clarified scope\"],\"implementation_checklist\":[\"Review with legal\"],\"risk_flags\":[\"Needs approval\"],\"confidence\":\"high\",\"quality_score\":92}");
        when(vertexAiService.composePolicyDraft(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("rewrite"), org.mockito.ArgumentMatchers.eq("Security Policy")))
                .thenReturn("{\"title\":\"Security Policy Rewrite\",\"summary\":\"Alternate\",\"draft\":\"Vertex draft\",\"rationale\":\"Alt rationale\",\"key_changes\":[\"Reordered sections\"],\"implementation_checklist\":[\"Check owners\"],\"risk_flags\":[\"Pending sign-off\"],\"confidence\":\"medium\",\"quality_score\":81}");

        PolicyComposeRequest request = new PolicyComposeRequest();
        request.setMode("rewrite");
        request.setProvider("both");
        request.setPolicyType("Security Policy");
        request.setGoals("Modernize the policy and make it publish-ready.");
        request.setSourceText("Old policy text.");
        request.setMustIncludeClauses(List.of("Incident escalation", "Access control"));

        Map<String, Object> response = service.composePolicy(request);

        assertEquals("openai", response.get("selectedProvider"));
        assertEquals("Security Policy Rewrite", response.get("title"));
        assertEquals(92, response.get("qualityScore"));
        assertTrue(response.get("providers") instanceof Map);
    }

    @Test
    public void composePolicy_requiresSourceTextForRewrite() {
        OpenAiService openAiService = mock(OpenAiService.class);
        VertexAiService vertexAiService = mock(VertexAiService.class);
        PolicyComposeService service = new PolicyComposeService(openAiService, vertexAiService, new ObjectMapper());

        PolicyComposeRequest request = new PolicyComposeRequest();
        request.setMode("rewrite");
        request.setPolicyType("Privacy Policy");
        request.setGoals("Make it stronger.");

        assertThrows(DocumentProcessingException.class, () -> service.composePolicy(request));
    }
}
