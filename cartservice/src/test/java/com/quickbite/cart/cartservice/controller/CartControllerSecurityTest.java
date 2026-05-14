package com.quickbite.cart.cartservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.cart.cartservice.config.SecurityConfig;
import com.quickbite.cart.cartservice.dto.responseDto.CartResponseDto;
import com.quickbite.cart.cartservice.exception.GlobalExceptionHandler;
import com.quickbite.cart.cartservice.security.JwtAuthenticationFilter;
import com.quickbite.cart.cartservice.security.UserPrincipal;
import com.quickbite.cart.cartservice.service.CartService;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CartControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void allowJwtFilterToContinueChain() throws Exception {
        doAnswer(invocation -> {
            invocation.<FilterChain>getArgument(2).doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void anonymousUserShouldBeRejected() throws Exception {
        mockMvc.perform(get("/cart/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerRoleShouldAccessCartEndpoint() throws Exception {
        when(cartService.getCartByCustomerId(1L)).thenReturn(new CartResponseDto());

        mockMvc.perform(get("/cart/me").with(authentication(customerAuth())))
                .andExpect(status().isOk());
    }

    @Test
    void wrongRoleShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemPayload(20L, 2, "No onion"))))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken customerAuth() {
        UserPrincipal principal = new UserPrincipal(1L, "customer@quickbite.com", "CUSTOMER");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(() -> "ROLE_CUSTOMER"));
    }

    private UsernamePasswordAuthenticationToken ownerAuth() {
        UserPrincipal principal = new UserPrincipal(2L, "owner@quickbite.com", "OWNER");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(() -> "ROLE_OWNER"));
    }

    private record CartItemPayload(Long menuItemId, Integer quantity, String customization) { }
}
