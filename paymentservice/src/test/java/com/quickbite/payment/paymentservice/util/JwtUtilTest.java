package com.quickbite.payment.paymentservice.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

    private static final String SECRET = "super-secret-key-for-payment-service-tests-12345";

    private JwtUtil jwtUtil;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void extractsClaimsFromValidToken() {
        String token = buildToken(17L, "customer@quickbite.com", "CUSTOMER", new Date(System.currentTimeMillis() + 60000));

        assertEquals(17L, jwtUtil.extractUserId(token));
        assertEquals("customer@quickbite.com", jwtUtil.extractEmail(token));
        assertEquals("CUSTOMER", jwtUtil.extractRole(token));
        assertTrue(jwtUtil.isTokenValid(token, "customer@quickbite.com"));
    }

    @Test
    void supportsIntegerUserIdClaim() {
        String token = Jwts.builder()
                .subject("customer@quickbite.com")
                .claim("userId", 21)
                .claim("role", "CUSTOMER")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(secretKey)
                .compact();

        assertEquals(21L, jwtUtil.extractUserId(token));
    }

    @Test
    void rejectsInvalidUserIdClaimType() {
        String token = Jwts.builder()
                .subject("customer@quickbite.com")
                .claim("userId", "not-a-number")
                .claim("role", "CUSTOMER")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(secretKey)
                .compact();

        assertThrows(RuntimeException.class, () -> jwtUtil.extractUserId(token));
    }

    @Test
    void returnsFalseForWrongEmailAndThrowsForExpiredToken() {
        String validToken = buildToken(17L, "customer@quickbite.com", "CUSTOMER",
                new Date(System.currentTimeMillis() + 60000));
        String expiredToken = buildToken(17L, "customer@quickbite.com", "CUSTOMER",
                new Date(System.currentTimeMillis() - 60000));

        assertFalse(jwtUtil.isTokenValid(validToken, "other@quickbite.com"));
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.isTokenValid(expiredToken, "customer@quickbite.com"));
    }

    private String buildToken(Long userId, String email, String role, Date expiration) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }
}
