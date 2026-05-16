package com.quickbite.payment.paymentservice.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.payment.paymentservice.dto.requestDto.AddToWalletRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayOrderRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayVerifyRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayWalletTopUpRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayWalletTopUpVerifyRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.WalletPaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.RazorpayOrderResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.RazorpayWalletTopUpOrderResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;
import com.quickbite.payment.paymentservice.security.UserPrincipal;
import com.quickbite.payment.paymentservice.service.PaymentService;
import com.quickbite.payment.paymentservice.service.RazorpayPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@Validated
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    private final RazorpayPaymentService razorpayPaymentService;

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

    @PostMapping("/payments/razorpay/create-order")
    public ResponseEntity<RazorpayOrderResponseDto> createRazorpayOrder(
            @Valid @RequestBody RazorpayOrderRequestDto request,
            Authentication authentication) {
            
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(razorpayPaymentService.createRazorpayOrder(request, user));
    }
    
    @PostMapping("/payments/razorpay/verify")
    public ResponseEntity<PaymentResponseDto> verifyRazorpayPayment(
            @Valid @RequestBody RazorpayVerifyRequestDto request,
            Authentication authentication) {
            
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(razorpayPaymentService.verifyRazorpayPayment(request, user));
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

    @PostMapping("/wallet/razorpay/create-order")
    public ResponseEntity<RazorpayWalletTopUpOrderResponseDto> createWalletTopUpOrder(
            @Valid @RequestBody RazorpayWalletTopUpRequestDto request,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(razorpayPaymentService.createWalletTopUpOrder(request, user));
    }

    @PostMapping("/wallet/razorpay/verify")
    public ResponseEntity<WalletResponseDto> verifyWalletTopUpPayment(
            @Valid @RequestBody RazorpayWalletTopUpVerifyRequestDto request,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(razorpayPaymentService.verifyWalletTopUpPayment(request, user));
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
