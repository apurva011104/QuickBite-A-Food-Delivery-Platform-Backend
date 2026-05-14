package com.quickbite.menu.menuservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

    private static final String SECRET = "12345678901234567890123456789012";

    private JwtUtil jwtUtil;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldExtractClaimsFromValidToken() {
        String token = Jwts.builder()
                .subject("owner@quickbite.com")
                .claim("userId", 7L)
                .claim("role", "OWNER")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("owner@quickbite.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(7L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("OWNER");
        assertThat(jwtUtil.isTokenValid(token, "owner@quickbite.com")).isTrue();
    }

    @Test
    void shouldThrowForExpiredToken() {
        String token = Jwts.builder()
                .subject("owner@quickbite.com")
                .claim("userId", 7L)
                .claim("role", "OWNER")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtUtil.isTokenValid(token, "owner@quickbite.com"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldRejectTokenForDifferentEmail() {
        String token = Jwts.builder()
                .subject("owner@quickbite.com")
                .claim("userId", 7L)
                .claim("role", "OWNER")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        assertThat(jwtUtil.isTokenValid(token, "someoneelse@quickbite.com")).isFalse();
    }
}
