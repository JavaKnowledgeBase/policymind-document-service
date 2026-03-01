package com.policymind.document.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri:http://localhost:5173/auth/callback}")
    private String frontendCallbackUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("error", "oauth_login_failed")
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
