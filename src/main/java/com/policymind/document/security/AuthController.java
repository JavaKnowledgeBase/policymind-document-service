package com.policymind.document.security;

import com.policymind.document.dto.AuthLoginRequest;
import com.policymind.document.dto.AuthRegisterRequest;
import com.policymind.document.dto.ResetPasswordRequest;
import com.policymind.document.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JwtService jwtService;
    private final AuthService authService;

    public AuthController(JwtService jwtService, AuthService authService) {
		this.jwtService = jwtService;
        this.authService = authService;
	}

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

	@PostMapping("/login")
    public String login(@RequestParam String username) {
        // This keeps the original lightweight login path for demos and existing clients.
        return jwtService.generateToken(username, "USER");
    }

    @PostMapping("/login/password")
    public ResponseEntity<Map<String, Object>> loginWithPassword(@RequestBody AuthLoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/forgot-password/question")
    public ResponseEntity<Map<String, Object>> getSecurityQuestion(@RequestParam String username) {
        return ResponseEntity.ok(authService.forgotPasswordQuestion(username));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        logger.warn("Auth request rejected: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
