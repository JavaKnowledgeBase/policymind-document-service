package com.policymind.document.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String requestUri = request.getRequestURI();
        final boolean traceAuth = shouldTraceAuth(requestUri);
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (traceAuth) {
                logger.info(
                        "No bearer token present for path='{}', method='{}', uploadRequestId='{}', questionRequestId='{}'",
                        requestUri,
                        request.getMethod(),
                        request.getHeader("X-Upload-Request-Id"),
                        request.getHeader("X-Question-Request-Id")
                );
            }
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = normalizeBearerToken(authHeader.substring(7));
        if (jwt.isEmpty()) {
            if (traceAuth) {
                logger.warn(
                        "Bearer token normalized to empty for path='{}', method='{}'",
                        requestUri,
                        request.getMethod()
                );
            }
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                String role = jwtService.extractClaim(jwt,
                        claims -> claims.get("role", String.class));

                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                if (traceAuth) {
                    logger.info(
                            "JWT accepted for path='{}', method='{}', username='{}', role='{}'",
                            requestUri,
                            request.getMethod(),
                            username,
                            role
                    );
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            logger.warn("Rejecting malformed JWT for path '{}': {}", requestUri, ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldTraceAuth(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        return requestUri.equals("/upload")
                || requestUri.startsWith("/documents/")
                || requestUri.endsWith("/ask");
    }

    private String normalizeBearerToken(String token) {
        String normalized = token == null ? "" : token.trim();
        while (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            normalized = normalized.substring(7).trim();
        }
        return normalized;
    }
}
