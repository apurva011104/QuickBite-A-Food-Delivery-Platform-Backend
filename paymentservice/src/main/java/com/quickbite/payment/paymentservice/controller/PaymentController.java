package com.quickbite.payment.paymentservice.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.quickbite.payment.paymentservice.dto.requestDto.AddToWalletRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.WalletPaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
import com.quickbite.payment.paymentservice.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto request,
                                                             Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.processPayment(request, user));
    }

    @GetMapping("/payments/order/{orderId}")
    public ResponseEntity<PaymentResponseDto> getByOrder(@PathVariable Long orderId,
                                                         Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.getPaymentByOrder(orderId, user));
    }

    @GetMapping("/payments/customer")
    public ResponseEntity<List<PaymentResponseDto>> getByCustomer(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.getPaymentsByCustomer(user.getUserId()));
    }

    @PutMapping("/payments/{paymentId}/status")
    public ResponseEntity<PaymentResponseDto> updateStatus(@PathVariable Long paymentId,
                                                           @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(paymentId, status));
    }

    @PostMapping("/payments/refund/{orderId}")
    public ResponseEntity<PaymentResponseDto> refund(@PathVariable Long orderId,
                                                     Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.refundPayment(orderId, user));
    }

    @GetMapping("/wallet")
    public ResponseEntity<WalletResponseDto> getWallet(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.getWallet(user.getUserId()));
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<BigDecimal> getBalance(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.getWalletBalance(user.getUserId()));
    }

    @PostMapping("/wallet/add")
    public ResponseEntity<WalletResponseDto> addMoney(@Valid @RequestBody AddToWalletRequestDto request,
                                                      Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.addToWallet(user.getUserId(), request.getAmount()));
    }

    @PostMapping("/wallet/pay")
    public ResponseEntity<PaymentResponseDto> payFromWallet(@Valid @RequestBody WalletPaymentRequestDto request,
                                                            Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.payFromWallet(user.getUserId(), request.getOrderId(), request.getAmount()));
    }

    @GetMapping("/wallet/statements")
    public ResponseEntity<List<WalletStatementResponseDto>> getStatements(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.getWalletStatements(user.getUserId()));
    }
}