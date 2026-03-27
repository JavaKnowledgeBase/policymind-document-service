package com.policymind.document.controller;

import com.policymind.document.service.PolicyComposeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PolicyComposeControllerTest {

    @Test
    public void composePolicy_returnsOkAndNoStoreHeaders() throws Exception {
        PolicyComposeService service = Mockito.mock(PolicyComposeService.class);
        when(service.composePolicy(any())).thenReturn(Map.of(
                "title", "Remote Work Policy",
                "qualityScore", 90,
                "selectedProvider", "openai"
        ));

        PolicyComposeController controller = new PolicyComposeController(service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/policy-compose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"create\",\"policyType\":\"Remote Work Policy\",\"goals\":\"Create a practical policy.\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.title").value("Remote Work Policy"))
                .andExpect(jsonPath("$.selectedProvider").value("openai"));
    }
}
