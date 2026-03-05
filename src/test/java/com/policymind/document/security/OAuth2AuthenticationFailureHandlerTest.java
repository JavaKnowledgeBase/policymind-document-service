package com.policymind.document.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.AuthenticationException;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class OAuth2AuthenticationFailureHandlerTest {

    @Test
    public void onAuthenticationFailure_redirectsWithErrorParam() throws Exception {
        OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler();
        java.lang.reflect.Field f = OAuth2AuthenticationFailureHandler.class.getDeclaredField("frontendCallbackUrl");
        f.setAccessible(true);
        f.set(handler, "http://localhost:5173/auth/callback");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        AuthenticationException ex = mock(AuthenticationException.class);

        handler.onAuthenticationFailure(req, resp, ex);

        ArgumentCaptor<String> capt = ArgumentCaptor.forClass(String.class);
        verify(resp).sendRedirect(capt.capture());
        String url = capt.getValue();
        assertTrue(url.contains("error=oauth_login_failed"));
    }
}
