package com.policymind.document.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthControllerTest {

    @Test
    public void login_returnsGeneratedToken() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.generateToken("alice", "USER")).thenReturn("token-xyz");

        AuthController controller = new AuthController(jwtService);

        // simulate request param binding by calling method directly
        String result = controller.login("alice");
        assertEquals("token-xyz", result);

        verify(jwtService).generateToken("alice", "USER");
    }
}
