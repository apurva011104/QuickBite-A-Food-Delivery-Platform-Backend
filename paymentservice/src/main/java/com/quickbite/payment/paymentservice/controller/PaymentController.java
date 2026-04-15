package com.quickbite.payment.paymentservice.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.payment.paymentservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;
import com.quickbite.payment.paymentservice.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    //Process Payment
    @PostMapping("/payments")
    public PaymentResponseDto processPayment(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody PaymentRequestDto request) {

        return paymentService.processPayment(request, token.substring(7));
    }

    //Get payment by orderId
    @GetMapping("/payments/order/{orderId}")
    public PaymentResponseDto getByOrder(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrder(orderId);
    }

    //Get payments by customer
    @GetMapping("/payments/customer")
    public List<PaymentResponseDto> getByCustomer(
            @RequestHeader("Authorization") String token) {

        return paymentService.getPaymentsByCustomer(token.substring(7));
    }

    //Update payment status
    @PutMapping("/payments/{paymentId}/status")
    public PaymentResponseDto updateStatus(
            @PathVariable Long paymentId,
            @RequestParam PaymentStatus status) {

        return paymentService.updatePaymentStatus(paymentId, status);
    }

    //Refund payment
    @PostMapping("/payments/refund/{orderId}")
    public PaymentResponseDto refund(@PathVariable Long orderId) {
        return paymentService.refundPayment(orderId);
    }

    //Get wallet details
    @GetMapping("/wallet")
    public WalletResponseDto getWallet(
            @RequestHeader("Authorization") String token) {

        return paymentService.getWallet(token.substring(7));
    }

    //Get wallet balance
    @GetMapping("/wallet/balance")
    public BigDecimal getBalance(
            @RequestHeader("Authorization") String token) {

        return paymentService.getWalletBalance(token.substring(7));
    }

    //Add money to wallet
    @PostMapping("/wallet/add")
    public WalletResponseDto addMoney(
            @RequestHeader("Authorization") String token,
            @RequestParam BigDecimal amount) {

        return paymentService.addToWallet(token.substring(7), amount);
    }

    //Pay from wallet
    @PostMapping("/wallet/pay")
    public PaymentResponseDto payFromWallet(
            @RequestHeader("Authorization") String token,
            @RequestParam Long orderId,
            @RequestParam BigDecimal amount) {

        return paymentService.payFromWallet(token.substring(7), orderId, amount);
    }

    //Get wallet statements
    @GetMapping("/wallet/statements")
    public List<WalletStatementResponseDto> getStatements(
            @RequestHeader("Authorization") String token) {

        return paymentService.getWalletStatements(token.substring(7));
    }
}