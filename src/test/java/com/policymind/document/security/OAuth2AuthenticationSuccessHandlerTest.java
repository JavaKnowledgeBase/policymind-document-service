package com.policymind.document.security;

import com.policymind.document.entity.User;
import com.policymind.document.enums.Role;
import com.policymind.document.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OAuth2AuthenticationSuccessHandlerTest {

    @Test
    public void onAuthenticationSuccess_createsUserAndRedirectsWithToken() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserRepository userRepository = mock(UserRepository.class);
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(jwtService, userRepository);

        // Set frontend callback URL since @Value won't be applied in this unit test
        java.lang.reflect.Field f = OAuth2AuthenticationSuccessHandler.class.getDeclaredField("frontendCallbackUrl");
        f.setAccessible(true);
        f.set(handler, "http://localhost:5173/auth/callback");

        Map<String, Object> attrs = Map.of("email", "u@example.com", "name", "User Name");
        DefaultOAuth2User principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), attrs, "email");
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");

        when(userRepository.findByUsername("u@example.com")).thenReturn(Optional.empty());
        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(savedCaptor.capture())).thenAnswer(inv -> {
            User u = (User) inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        when(jwtService.generateToken("u@example.com", Role.USER.name())).thenReturn("tok123");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        handler.onAuthenticationSuccess(req, resp, token);

        verify(userRepository).findByUsername("u@example.com");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken("u@example.com", Role.USER.name());
        verify(resp).sendRedirect(contains("token=tok123"));

        User saved = savedCaptor.getValue();
        assertEquals("u@example.com", saved.getUsername());
        assertNull(saved.getPassword());
        assertEquals(Role.USER, saved.getRole());
    }
}
