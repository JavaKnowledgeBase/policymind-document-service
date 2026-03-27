package com.policymind.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenAiServiceTest {

    @Test
    public void askLLM_returnsResponseString() {
        RestTemplate rt = mock(RestTemplate.class);
        OutboundCallExecutor outboundCallExecutor = mock(OutboundCallExecutor.class);
        OpenAiService svc = new OpenAiService("test-key", rt, outboundCallExecutor, new ObjectMapper());

        String fake = "{\"choices\":[{\"message\":{\"content\":\"{\\\"summary\\\":\\\"ok\\\",\\\"answer\\\":\\\"yes\\\",\\\"risk_score\\\":1,\\\"confidence\\\":\\\"high\\\",\\\"key_risks\\\":[],\\\"recommended_actions\\\":[] }\"}}]}";
        when(rt.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(fake));
        when(outboundCallExecutor.execute(eq("openai-chat"), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Supplier<String> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        String resp = svc.askLLM("context", "q");
        assertNotNull(resp);
        assertTrue(resp.contains("answer"));
        verify(rt, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }
}
