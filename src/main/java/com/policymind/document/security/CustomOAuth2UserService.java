package com.policymind.document.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        if (!"twitter".equals(provider)) {
            return user;
        }

        Map<String, Object> attributes = new HashMap<>(user.getAttributes());
        Object rawData = attributes.get("data");
        if (rawData instanceof Map<?, ?> data) {
            if (data.get("id") != null) {
                attributes.put("id", data.get("id").toString());
            }
            if (data.get("username") != null) {
                attributes.put("username", data.get("username").toString());
            }
            if (data.get("name") != null) {
                attributes.put("name", data.get("name").toString());
            }
        }

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        return new DefaultOAuth2User(authorities, attributes, "id");
    }
}
