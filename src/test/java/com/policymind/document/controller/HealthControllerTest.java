package com.policymind.document.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthControllerTest {

    @Test
    public void healthEndpoint_returnsOk() throws Exception {
        HealthController hc = new HealthController();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(hc).build();

        mvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
