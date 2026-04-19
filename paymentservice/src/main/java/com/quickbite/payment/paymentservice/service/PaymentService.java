package com.quickbite.payment.paymentservice.service;

import java.math.BigDecimal;
import java.util.List;

import com.quickbite.payment.paymentservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;
import com.quickbite.payment.paymentservice.security.UserPrincipal;

public interface PaymentService {

    PaymentResponseDto processPayment(PaymentRequestDto request, UserPrincipal currentUser);

    PaymentResponseDto getPaymentByOrder(Long orderId, UserPrincipal currentUser);

    List<PaymentResponseDto> getPaymentsByCustomer(Long customerId);

    PaymentResponseDto updatePaymentStatus(Long paymentId, PaymentStatus status);

    PaymentResponseDto refundPayment(Long orderId, UserPrincipal currentUser);

    WalletResponseDto getWallet(Long customerId);

    BigDecimal getWalletBalance(Long customerId);

    WalletResponseDto addToWallet(Long customerId, BigDecimal amount);

    PaymentResponseDto payFromWallet(Long customerId, Long orderId, BigDecimal amount);

    List<WalletStatementResponseDto> getWalletStatements(Long customerId);
}