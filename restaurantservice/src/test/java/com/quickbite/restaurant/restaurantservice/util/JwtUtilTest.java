package com.quickbite.restaurant.restaurantservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static final String SECRET = "12345678901234567890123456789012";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    @Test
    void shouldExtractClaimsFromValidToken() {
        String token = createToken(new Date(System.currentTimeMillis() + 60_000));

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("owner@quickbite.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(99L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("OWNER");
        assertThat(jwtUtil.isTokenValid(token, "owner@quickbite.com")).isTrue();
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = createToken(new Date(System.currentTimeMillis() - 60_000));

        assertThatThrownBy(() -> jwtUtil.isTokenValid(token, "owner@quickbite.com"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldFailWhenUserIdClaimIsUnsupportedType() {
        SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("owner@quickbite.com")
                .claim("userId", "wrong-type")
                .claim("role", "OWNER")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey)
                .compact();

        assertThatThrownBy(() -> jwtUtil.extractUserId(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid userId in token");
    }

    private String createToken(Date expiration) {
        SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("owner@quickbite.com")
                .claim("userId", 99L)
                .claim("role", "OWNER")
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }
}
