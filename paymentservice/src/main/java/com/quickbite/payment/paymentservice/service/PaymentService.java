package com.quickbite.payment.paymentservice.service;

import java.math.BigDecimal;
import java.util.List;

import com.quickbite.payment.paymentservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.WalletStatementResponseDto;
import com.quickbite.payment.paymentservice.entity.PaymentStatus;

public interface PaymentService {

    PaymentResponseDto processPayment(PaymentRequestDto request, String token);

    PaymentResponseDto getPaymentByOrder(Long orderId);

    List<PaymentResponseDto> getPaymentsByCustomer(String token);

    PaymentResponseDto updatePaymentStatus(Long paymentId, PaymentStatus status);

    PaymentResponseDto refundPayment(Long orderId);

    WalletResponseDto getWallet(String token);

    BigDecimal getWalletBalance(String token);

    WalletResponseDto addToWallet(String token, BigDecimal amount);

    PaymentResponseDto payFromWallet(String token, Long orderId, BigDecimal amount);

    List<WalletStatementResponseDto> getWalletStatements(String token);
}