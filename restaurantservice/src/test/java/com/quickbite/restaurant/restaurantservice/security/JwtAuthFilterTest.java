package com.quickbite.restaurant.restaurantservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickbite.restaurant.restaurantservice.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter();
        ReflectionTestUtils.setField(jwtAuthFilter, "jwtUtil", jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBypassPublicEndpointsWithoutJwtParsing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/restaurants/public/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldAuthenticateValidBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/restaurants/owner/my");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractEmail("valid-token")).thenReturn("owner@quickbite.com");
        when(jwtUtil.isTokenValid("valid-token", "owner@quickbite.com")).thenReturn(true);
        when(jwtUtil.extractUserId("valid-token")).thenReturn(42L);
        when(jwtUtil.extractRole("valid-token")).thenReturn("OWNER");

        jwtAuthFilter.doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_OWNER");
        assertThat(((UserPrincipal) authentication.getPrincipal()).getUserId()).isEqualTo(42L);
    }

    @Test
    void shouldReturnUnauthorizedWhenJwtParsingFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/restaurants/admin/pending");
        request.addHeader("Authorization", "Bearer broken-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractEmail("broken-token")).thenThrow(new RuntimeException("bad token"));

        jwtAuthFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil).extractEmail("broken-token");
    }
}
