package com.quickbite.payment.paymentservice.mapper;

import com.quickbite.payment.paymentservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.payment.paymentservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.payment.paymentservice.entity.Payment;

public class PaymentMapper {

    public static Payment dtoToEntity(PaymentRequestDto dto, Long customerId) {
        return new Payment(
                dto.getOrderId(),
                customerId,
                dto.getAmount(),
                dto.getMode()
        );
    }

    public static PaymentResponseDto entityToDto(Payment payment) {
        return new PaymentResponseDto(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMode(),
                payment.getTransactionId(),
                payment.getCurrency(),
                payment.getPaidAt(),
                payment.getRefundedAt(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId()
        );
    }
}