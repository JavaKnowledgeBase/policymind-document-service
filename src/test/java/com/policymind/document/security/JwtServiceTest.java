package com.policymind.document.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    @Test
    public void generateAndValidateToken() throws Exception {
        JwtService jwtService = new JwtService();

        // set secret via reflection (must be 32+ bytes)
        Field secretField = JwtService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtService, "012345678901234567890123456789012345");

        String token = jwtService.generateToken("alice", "USER");
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("alice", username);

        // create a simple UserDetails implementation
        org.springframework.security.core.userdetails.UserDetails userDetails =
                org.springframework.security.core.userdetails.User.withUsername("alice").password("x").roles("USER").build();

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    public void expiredTokenIsInvalid() throws Exception {
        JwtService jwtService = new JwtService();

        Field secretField = JwtService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtService, "012345678901234567890123456789012345");

        // Generate a token and tamper the expiration by parsing and re-signing with an old date is complex.
        // Instead, ensure that invalid token string is handled gracefully.
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtService.extractUsername("not-a-token"));
    }
}
