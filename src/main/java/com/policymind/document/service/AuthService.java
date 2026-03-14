package com.policymind.document.service;

import com.policymind.document.dto.AuthLoginRequest;
import com.policymind.document.dto.AuthRegisterRequest;
import com.policymind.document.dto.ResetPasswordRequest;
import com.policymind.document.entity.User;
import com.policymind.document.enums.Role;
import com.policymind.document.repository.UserRepository;
import com.policymind.document.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> register(AuthRegisterRequest request) {
        String username = normalizedValue(request.getUsername());
        String password = normalizedValue(request.getPassword());
        String securityQuestion = normalizedValue(request.getSecurityQuestion());
        String securityAnswer = normalizedValue(request.getSecurityAnswer());

        validateRequired(username, "Username is required.");
        validateRequired(password, "Password is required.");
        validateRequired(securityQuestion, "Security question is required.");
        validateRequired(securityAnswer, "Security answer is required.");

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswerHash(passwordEncoder.encode(normalizeSecurityAnswer(securityAnswer)));
        user.setRole(Role.USER);
        userRepository.save(user);

        logger.info("Registered local user '{}'", username);

        return Map.of(
                "message", "User registered successfully.",
                "username", username,
                "role", Role.USER.name()
        );
    }

    public String login(AuthLoginRequest request) {
        String username = normalizedValue(request.getUsername());
        String password = normalizedValue(request.getPassword());

        validateRequired(username, "Username is required.");
        validateRequired(password, "Password is required.");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("Failed password login attempt for username='{}'", username);
            throw new IllegalArgumentException("Invalid username or password.");
        }

        logger.info("Password login succeeded for username='{}'", username);
        return jwtService.generateToken(username, user.getRole().name());
    }

    public Map<String, Object> forgotPasswordQuestion(String username) {
        String normalizedUsername = normalizedValue(username);
        validateRequired(normalizedUsername, "Username is required.");

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        logger.info("Security question requested for username='{}'", normalizedUsername);

        Map<String, Object> response = new HashMap<>();
        response.put("username", normalizedUsername);
        response.put("securityQuestion", user.getSecurityQuestion());
        return response;
    }

    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        String username = normalizedValue(request.getUsername());
        String securityAnswer = normalizedValue(request.getSecurityAnswer());
        String newPassword = normalizedValue(request.getNewPassword());

        validateRequired(username, "Username is required.");
        validateRequired(securityAnswer, "Security answer is required.");
        validateRequired(newPassword, "New password is required.");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String storedAnswerHash = user.getSecurityAnswerHash();
        if (storedAnswerHash == null || !passwordEncoder.matches(normalizeSecurityAnswer(securityAnswer), storedAnswerHash)) {
            logger.warn("Security question verification failed for username='{}'", username);
            throw new IllegalArgumentException("Security answer is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password reset completed for username='{}'", username);

        return Map.of(
                "message", "Password reset successful.",
                "username", username
        );
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizedValue(String value) {
        return value == null ? null : value.trim();
    }

    // Normalize user-entered answers so reset checks are case-insensitive and whitespace-tolerant.
    private String normalizeSecurityAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase();
    }
}
