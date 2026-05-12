package com.quickbite.payment.paymentservice.service;

import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayOrderRequestDto;
import com.quickbite.payment.paymentservice.dto.requestDto.RazorpayVerifyRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.dto.responseDto.RazorpayOrderResponseDto;
import com.quickbite.payment.paymentservice.security.UserPrincipal;

public interface RazorpayPaymentService {

    RazorpayOrderResponseDto createRazorpayOrder(RazorpayOrderRequestDto request, UserPrincipal currentUser);

    PaymentResponseDto verifyRazorpayPayment(RazorpayVerifyRequestDto request, UserPrincipal currentUser);
}