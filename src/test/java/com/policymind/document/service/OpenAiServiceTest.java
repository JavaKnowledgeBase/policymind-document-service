package com.policymind.document.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OpenAiServiceTest {

    @Test
    public void askLLM_returnsResponseString() {
        RestTemplate rt = mock(RestTemplate.class);
        OpenAiService svc = new OpenAiService();

        // inject apiKey and mock restTemplate via reflection
        try {
            java.lang.reflect.Field apiKeyField = OpenAiService.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(svc, "test-key");

            java.lang.reflect.Field rtField = OpenAiService.class.getDeclaredField("restTemplate");
            rtField.setAccessible(true);
            rtField.set(svc, rt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String fake = "{\"choices\":[{\"message\":{\"content\":\"{\\\"summary\\\":\\\"ok\\\",\\\"answer\\\":\\\"yes\\\",\\\"risk_score\\\":1,\\\"confidence\\\":\\\"high\\\",\\\"key_risks\\\":[],\\\"recommended_actions\\\":[] }\"}}]}";
        when(rt.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok(fake));

        String resp = svc.askLLM("context","q");
        assertNotNull(resp);
        assertTrue(resp.contains("answer"));
        verify(rt, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }
}
