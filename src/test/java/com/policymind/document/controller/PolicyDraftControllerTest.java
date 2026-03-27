package com.policymind.document.controller;

import com.policymind.document.service.PolicyDraftService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PolicyDraftControllerTest {

    @Test
    public void saveDraft_returnsOk() throws Exception {
        PolicyDraftService service = Mockito.mock(PolicyDraftService.class);
        when(service.saveDraft(any())).thenReturn(Map.of("draftId", 15L, "currentVersionNumber", 1));

        PolicyDraftController controller = new PolicyDraftController(service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/policy-drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyType\":\"Security Policy\",\"workingDraft\":\"draft text\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.draftId").value(15L));
    }

    @Test
    public void listDrafts_returnsOk() throws Exception {
        PolicyDraftService service = Mockito.mock(PolicyDraftService.class);
        when(service.listDrafts()).thenReturn(List.of(Map.of("draftId", 15L, "title", "Security Policy Draft")));

        PolicyDraftController controller = new PolicyDraftController(service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/policy-drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].draftId").value(15L));
    }
}
