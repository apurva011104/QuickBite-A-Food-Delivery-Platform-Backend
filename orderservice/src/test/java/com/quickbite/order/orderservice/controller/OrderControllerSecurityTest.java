package com.quickbite.order.orderservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.quickbite.order.orderservice.config.SecurityConfig;
import com.quickbite.order.orderservice.exception.GlobalExceptionHandler;
import com.quickbite.order.orderservice.security.JwtAuthenticationFilter;
import com.quickbite.order.orderservice.security.UserPrincipal;
import com.quickbite.order.orderservice.service.OrderService;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

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
    void anonymousUserShouldBeRejectedForCustomerEndpoint() throws Exception {
        mockMvc.perform(get("/orders/customer"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerShouldAccessOwnOrdersEndpoint() throws Exception {
        when(orderService.getOrdersByCustomer(1L, "token123")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/orders/customer")
                        .header("Authorization", "Bearer token123")
                        .with(customerPrincipal()))
                .andExpect(status().isOk());
    }

    @Test
    void ownerShouldAccessRestaurantEndpoint() throws Exception {
        when(orderService.getOrdersByRestaurant(org.mockito.ArgumentMatchers.eq(10L), any(UserPrincipal.class)))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/orders/restaurant/10").with(ownerPrincipal()))
                .andExpect(status().isOk());
    }

    @Test
    void ownerShouldNotPlaceCustomerOrder() throws Exception {
        mockMvc.perform(post("/orders").with(ownerPrincipal()))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor customerPrincipal() {
        UserPrincipal principal = new UserPrincipal(1L, "customer@quickbite.com", "CUSTOMER");
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of(() -> "ROLE_CUSTOMER")));
        return securityContext(context);
    }

    private RequestPostProcessor ownerPrincipal() {
        UserPrincipal principal = new UserPrincipal(2L, "owner@quickbite.com", "OWNER");
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of(() -> "ROLE_OWNER")));
        return securityContext(context);
    }
}
