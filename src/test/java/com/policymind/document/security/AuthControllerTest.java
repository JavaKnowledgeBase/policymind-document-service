package com.policymind.document.security;

import com.policymind.document.dto.AuthLoginRequest;
import com.policymind.document.dto.AuthRegisterRequest;
import com.policymind.document.dto.ResetPasswordRequest;
import com.policymind.document.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthControllerTest {

    @Test
    public void login_returnsGeneratedToken() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = mock(AuthService.class);
        when(jwtService.generateToken("alice", "USER")).thenReturn("token-xyz");

        AuthController controller = new AuthController(jwtService, authService);

        // simulate request param binding by calling method directly
        String result = controller.login("alice");
        assertEquals("token-xyz", result);

        verify(jwtService).generateToken("alice", "USER");
    }

    @Test
    public void register_delegatesToAuthService() {
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(jwtService, authService);

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        request.setSecurityQuestion("Pet?");
        request.setSecurityAnswer("Milo");

        when(authService.register(request)).thenReturn(Map.of("message", "User registered successfully."));

        ResponseEntity<Map<String, Object>> response = controller.register(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("User registered successfully.", response.getBody().get("message"));
    }

    @Test
    public void loginWithPassword_returnsTokenPayload() {
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(jwtService, authService);

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        when(authService.login(request)).thenReturn("jwt-token");

        ResponseEntity<Map<String, Object>> response = controller.loginWithPassword(request);
        assertEquals("jwt-token", response.getBody().get("token"));
    }

    @Test
    public void resetPassword_delegatesToAuthService() {
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(jwtService, authService);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUsername("alice");
        request.setSecurityAnswer("milo");
        request.setNewPassword("new-secret");

        when(authService.resetPassword(request)).thenReturn(Map.of("message", "Password reset successful."));

        ResponseEntity<Map<String, Object>> response = controller.resetPassword(request);
        assertEquals("Password reset successful.", response.getBody().get("message"));
    }
}
