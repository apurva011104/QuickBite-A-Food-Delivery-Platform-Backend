package com.quickbite.order.orderservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.quickbite.order.orderservice.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipAuthenticationWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldAuthenticateValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractEmail("valid-token")).thenReturn("customer@quickbite.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("CUSTOMER");
        when(jwtUtil.extractUserId("valid-token")).thenReturn(1L);
        when(jwtUtil.isTokenValid("valid-token", "customer@quickbite.com")).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractEmail("bad-token")).thenThrow(new RuntimeException("bad token"));

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        verify(jwtUtil).extractEmail("bad-token");
    }
}
