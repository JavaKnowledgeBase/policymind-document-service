package com.policymind.document.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import java.time.Instant;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomOAuth2UserServiceTest {

    @Test
    public void twitterProvider_mapsDataAttributes() throws Exception {
        CustomOAuth2UserService svc = new CustomOAuth2UserService();

        // create mock delegate and inject via reflection
        DefaultOAuth2UserService delegate = mock(DefaultOAuth2UserService.class);
        java.lang.reflect.Field f = CustomOAuth2UserService.class.getDeclaredField("delegate");
        f.setAccessible(true);
        f.set(svc, delegate);

        Map<String,Object> data = new HashMap<>();
        data.put("id", "123");
        data.put("username", "tuser");
        data.put("name", "T Name");

        Map<String,Object> attrs = new HashMap<>();
        attrs.put("data", data);

        OAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), attrs, "data");
        when(delegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(user);

        ClientRegistration reg = ClientRegistration.withRegistrationId("twitter").authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE).authorizationUri("http://auth").redirectUri("http://localhost").tokenUri("http://token").clientId("x").clientSecret("s").scope("read").userInfoUri("http://user").build();
        OAuth2AccessToken tokenObj = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "at", Instant.now(), Instant.now().plusSeconds(3600));
        OAuth2UserRequest req = new OAuth2UserRequest(reg, tokenObj);

        OAuth2User out = svc.loadUser(req);
        assertEquals("123", out.getAttribute("id"));
        assertEquals("tuser", out.getAttribute("username"));
        assertEquals("T Name", out.getAttribute("name"));
    }

    @Test
    public void nonTwitterProvider_returnsDelegateUser() throws Exception {
        CustomOAuth2UserService svc = new CustomOAuth2UserService();
        DefaultOAuth2UserService delegate = mock(DefaultOAuth2UserService.class);
        java.lang.reflect.Field f = CustomOAuth2UserService.class.getDeclaredField("delegate");
        f.setAccessible(true);
        f.set(svc, delegate);

        Map<String,Object> attrs = Map.of("email","e@x.com");
        OAuth2User user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), attrs, "email");
        when(delegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(user);

        ClientRegistration reg = ClientRegistration.withRegistrationId("github").authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE).authorizationUri("http://auth").redirectUri("http://localhost").tokenUri("http://token").clientId("x").clientSecret("s").scope("read").userInfoUri("http://user").build();
        OAuth2AccessToken tokenObj2 = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "at2", Instant.now(), Instant.now().plusSeconds(3600));
        OAuth2UserRequest req = new OAuth2UserRequest(reg, tokenObj2);

        OAuth2User out = svc.loadUser(req);
        assertEquals("e@x.com", out.getAttribute("email"));
    }
}
