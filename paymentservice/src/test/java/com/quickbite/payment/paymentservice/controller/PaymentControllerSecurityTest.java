package com.quickbite.payment.paymentservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.payment.paymentservice.config.SecurityConfig;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.security.JwtAuthenticationFilter;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
import com.quickbite.payment.paymentservice.service.PaymentService;
import com.quickbite.payment.paymentservice.service.RazorpayPaymentService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private RazorpayPaymentService razorpayPaymentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken customerAuth;
    private UsernamePasswordAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        when(paymentService.getWallet(1L)).thenReturn(new WalletResponseDto(1L, 1L, BigDecimal.TEN, List.of()));
        when(paymentService.processPayment(any(), any())).thenReturn(new PaymentResponseDto());
        when(paymentService.updatePaymentStatus(any(), any())).thenReturn(new PaymentResponseDto());

        customerAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(1L, "customer@quickbite.com", "CUSTOMER"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        adminAuth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(2L, "admin@quickbite.com", "ADMIN"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void customerCanProcessPayment() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new PaymentRequestBody(600L, new BigDecimal("20.00"), "CARD"));

        mockMvc.perform(post("/payments")
                        .with(authentication(customerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void customerCanAccessWalletEndpoints() throws Exception {
        mockMvc.perform(get("/wallet").with(authentication(customerAuth)))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotUpdatePaymentStatus() throws Exception {
        mockMvc.perform(put("/payments/5/status")
                        .param("status", "PAID")
                        .with(authentication(customerAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdatePaymentStatus() throws Exception {
        mockMvc.perform(put("/payments/5/status")
                        .param("status", "PAID")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserIsRejected() throws Exception {
        mockMvc.perform(get("/wallet"))
                .andExpect(status().isForbidden());
    }

    private record PaymentRequestBody(Long orderId, BigDecimal amount, String mode) {
    }
}
