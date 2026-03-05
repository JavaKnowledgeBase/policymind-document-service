package com.policymind.document.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtAuthenticationFilterTest {

    @AfterEach
    public void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void noAuthorizationHeader_callsChain() throws Exception {
        JwtService jwt = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(req, resp, chain);

        verify(chain, times(1)).doFilter(req, resp);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void validToken_setsAuthentication() throws Exception {
        JwtService jwt = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwt.extractUsername("validtoken")).thenReturn("alice");
        when(jwt.extractClaim(eq("validtoken"), any())).thenReturn("USER");

        filter.doFilterInternal(req, resp, chain);

        verify(chain, times(1)).doFilter(req, resp);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("alice", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    public void jwtThrows_clearsContext_and_callsChain() throws Exception {
        JwtService jwt = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn("Bearer badtoken");
        when(jwt.extractUsername("badtoken")).thenThrow(new JwtException("bad"));

        filter.doFilterInternal(req, resp, chain);

        verify(chain, times(1)).doFilter(req, resp);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
