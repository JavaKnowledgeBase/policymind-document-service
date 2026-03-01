package com.policymind.document.security;

import com.policymind.document.entity.User;
import com.policymind.document.enums.Role;
import com.policymind.document.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:5173/auth/callback}")
    private String frontendCallbackUrl;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        String username = resolveUsername(oauth2User.getAttributes(), oauthToken.getAuthorizedClientRegistrationId());
        userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword(null);
            user.setRole(Role.USER);
            return userRepository.save(user);
        });

        String token = jwtService.generateToken(username, Role.USER.name());
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("token", token)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String resolveUsername(Map<String, Object> attributes, String provider) {
        String[] keys = {"email", "preferred_username", "login", "name", "sub", "id"};
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return provider + "_user";
    }
}
