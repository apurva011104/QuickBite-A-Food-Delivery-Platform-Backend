package com.quickbite.payment.paymentservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.quickbite.payment.paymentservice.dto.requestDto.AddToWalletRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayOrderRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayVerifyRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.WalletPaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.RazorpayOrderResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.PaymentMode;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;
import com.quickbite.payment.paymentservice.entity.TransactionType;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
import com.quickbite.payment.paymentservice.service.PaymentService;
import com.quickbite.payment.paymentservice.service.RazorpayPaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private RazorpayPaymentService razorpayPaymentService;

    @Mock
    private Authentication authentication;

    private PaymentController paymentController;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        paymentController = new PaymentController(paymentService, razorpayPaymentService);
        principal = new UserPrincipal(5L, "customer@quickbite.com", "CUSTOMER");
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Test
    void processPaymentDelegatesToService() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(500L);
        request.setAmount(new BigDecimal("90.00"));
        request.setMode(PaymentMode.CARD);
        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setOrderId(500L);
        when(paymentService.processPayment(request, principal)).thenReturn(responseDto);

        ResponseEntity<PaymentResponseDto> response = paymentController.processPayment(request, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(500L, response.getBody().getOrderId());
    }

    @Test
    void getByOrderDelegatesToService() {
        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setStatus(PaymentStatus.PAID);
        when(paymentService.getPaymentByOrder(501L, principal)).thenReturn(responseDto);

        ResponseEntity<PaymentResponseDto> response = paymentController.getByOrder(501L, authentication);

        assertEquals(PaymentStatus.PAID, response.getBody().getStatus());
    }

    @Test
    void createRazorpayOrderDelegatesToService() {
        RazorpayOrderRequestDto request = new RazorpayOrderRequestDto();
        request.setOrderId(502L);
        request.setAmount(new BigDecimal("100.00"));
        RazorpayOrderResponseDto responseDto = new RazorpayOrderResponseDto("order_502", "key", new BigDecimal("100.00"), "INR");
        when(razorpayPaymentService.createRazorpayOrder(request, principal)).thenReturn(responseDto);

        ResponseEntity<RazorpayOrderResponseDto> response = paymentController.createRazorpayOrder(request, authentication);

        assertEquals("order_502", response.getBody().getRazorpayOrderId());
    }

    @Test
    void verifyRazorpayPaymentDelegatesToService() {
        RazorpayVerifyRequestDto request = new RazorpayVerifyRequestDto();
        request.setOrderId(503L);
        request.setRazorpayOrderId("order_503");
        request.setRazorpayPaymentId("pay_503");
        request.setRazorpaySignature("sig_503");
        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setTransactionId("pay_503");
        when(razorpayPaymentService.verifyRazorpayPayment(request, principal)).thenReturn(responseDto);

        ResponseEntity<PaymentResponseDto> response = paymentController.verifyRazorpayPayment(request, authentication);

        assertEquals("pay_503", response.getBody().getTransactionId());
    }

    @Test
    void addMoneyUsesCurrentUsersId() {
        AddToWalletRequestDto request = new AddToWalletRequestDto();
        request.setAmount(new BigDecimal("55.00"));
        WalletResponseDto responseDto = new WalletResponseDto(7L, principal.getUserId(), new BigDecimal("55.00"), List.of());
        when(paymentService.addToWallet(principal.getUserId(), new BigDecimal("55.00"))).thenReturn(responseDto);

        ResponseEntity<WalletResponseDto> response = paymentController.addMoney(request, authentication);

        assertEquals(new BigDecimal("55.00"), response.getBody().getBalance());
    }

    @Test
    void payFromWalletUsesCurrentUsersId() {
        WalletPaymentRequestDto request = new WalletPaymentRequestDto();
        request.setOrderId(504L);
        request.setAmount(new BigDecimal("45.00"));
        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setStatus(PaymentStatus.PAID);
        when(paymentService.payFromWallet(principal.getUserId(), 504L, new BigDecimal("45.00"))).thenReturn(responseDto);

        ResponseEntity<PaymentResponseDto> response = paymentController.payFromWallet(request, authentication);

        assertEquals(PaymentStatus.PAID, response.getBody().getStatus());
    }

    @Test
    void getStatementsDelegatesToService() {
        WalletStatementResponseDto statement = new WalletStatementResponseDto(1L, new BigDecimal("15.00"),
                TransactionType.CREDIT, "Wallet top-up", java.time.LocalDateTime.now());
        when(paymentService.getWalletStatements(principal.getUserId())).thenReturn(List.of(statement));

        ResponseEntity<List<WalletStatementResponseDto>> response = paymentController.getStatements(authentication);

        assertEquals(1, response.getBody().size());
        verify(paymentService).getWalletStatements(principal.getUserId());
    }
}
