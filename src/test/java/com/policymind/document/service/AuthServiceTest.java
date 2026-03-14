package com.policymind.document.service;

import com.policymind.document.dto.AuthLoginRequest;
import com.policymind.document.dto.AuthRegisterRequest;
import com.policymind.document.dto.ResetPasswordRequest;
import com.policymind.document.entity.User;
import com.policymind.document.enums.Role;
import com.policymind.document.repository.UserRepository;
import com.policymind.document.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Test
    public void register_hashesPasswordAndSecurityAnswer() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(userRepository, jwtService, passwordEncoder);

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        request.setSecurityQuestion("Favorite pet?");
        request.setSecurityAnswer("Milo");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed-password");
        when(passwordEncoder.encode("milo")).thenReturn("hashed-answer");

        Map<String, Object> response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals("alice", savedUser.getUsername());
        assertEquals("hashed-password", savedUser.getPassword());
        assertEquals("Favorite pet?", savedUser.getSecurityQuestion());
        assertEquals("hashed-answer", savedUser.getSecurityAnswerHash());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals("User registered successfully.", response.get("message"));
    }

    @Test
    public void login_returnsJwtForValidPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(userRepository, jwtService, passwordEncoder);

        User user = new User();
        user.setUsername("alice");
        user.setPassword("hashed-password");
        user.setRole(Role.USER);

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("alice", "USER")).thenReturn("jwt-token");

        assertEquals("jwt-token", authService.login(request));
    }

    @Test
    public void resetPassword_updatesPasswordAfterSecurityAnswerCheck() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(userRepository, jwtService, passwordEncoder);

        User user = new User();
        user.setUsername("alice");
        user.setSecurityAnswerHash("hashed-answer");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUsername("alice");
        request.setSecurityAnswer("Milo");
        request.setNewPassword("new-secret");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("milo", "hashed-answer")).thenReturn(true);
        when(passwordEncoder.encode("new-secret")).thenReturn("new-password-hash");

        Map<String, Object> response = authService.resetPassword(request);

        verify(userRepository).save(user);
        assertEquals("new-password-hash", user.getPassword());
        assertEquals("Password reset successful.", response.get("message"));
    }
}
